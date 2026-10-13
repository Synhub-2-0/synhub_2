# Auditoría técnica de SynHub

| | |
|---|---|
| **Fecha** | 2026-10-13 |
| **Alcance** | Monorepo completo: `backend/`, `frontend/`, Docker, CI/CD y documentación |
| **Commit auditado** | `68536da` (rama `main`) |
| **Estado** | 48 hallazgos pendientes y 6 decisiones humanas |

## Cómo usar este documento (instrucciones para el prompt de corrección)

1. **Trabajar por fases**, en el orden de la sección [Plan de ejecución](#plan-de-ejecución). Dentro de una fase, seguir el orden de los IDs.
2. **Un hallazgo por commit**, con el mensaje `fix(<ID>): <resumen>` (o `refactor(<ID>)`, `chore(<ID>)`, `test(<ID>)`).
3. **Cada corrección debe cumplir sus "Criterios de aceptación"**. Si el hallazgo es un defecto reproducible, primero se escribe un test que falle y después se corrige.
4. **Después de cada fase** deben pasar:
   ```bash
   cd backend && ./mvnw -B verify
   cd frontend && npx ng test --watch=false && npx ng build
   ```
5. **No ejecutar** los puntos de [Decisiones humanas](#decisiones-humanas-no-automatizar) (`HUM-*`): requieren credenciales, acceso a AWS o una decisión de producto. Si un hallazgo depende de una de ellas, implementar la opción marcada como **recomendada** y dejarlo indicado.
6. **Mantener la documentación**: al corregir, actualizar las secciones "Observaciones" de `docs/features/<feature>/…` y `docs/architecture/…` que mencionan el problema, y cambiar la columna **Estado** del [índice](#índice-de-hallazgos) a `Resuelto` (o `Descartado`, con el motivo).

## Metodología

| Técnica | Detalle |
|---|---|
| Revisión de código | Los 220 archivos Java y los 95 archivos TypeScript (sin specs), la configuración, los Dockerfiles y los workflows. |
| Reproducción dinámica | Se levantó el stack con `docker compose` y se ejecutó un script con cuatro usuarios (líder, miembro, ajeno e invitado) contra la API real. Los hallazgos marcados **Reproducido** muestran el código HTTP obtenido. |
| Dependencias | `npm audit` en `frontend/`. Revisión manual de versiones en `pom.xml`. |
| Pruebas existentes | `./mvnw verify` (28/28 pasan) y `ng test` (25/25 pasan). |
| Logs | Inspección de `docker logs` del contenedor de la app durante la reproducción. |

Severidades:

- **Crítica**: explotable por cualquier usuario registrado, con impacto en la confidencialidad o la integridad.
- **Alta**: rompe un flujo principal o expone secretos.
- **Media**: defecto real con impacto acotado.
- **Baja**: calidad, mantenibilidad o higiene.

## Resumen

| Severidad | Cantidad |
|---|---|
| Crítica | 2 |
| Alta | 7 |
| Media | 17 |
| Baja | 22 |
| **Total** | **48** |

Los riesgos principales son tres:

1. **Faltan controles de acceso en las lecturas.** Cualquier usuario registrado (el registro es abierto) puede leer las tareas, los integrantes, las invitaciones y las entregas de **cualquier** grupo, y borrar entregas ajenas.
2. **Las credenciales quedan expuestas en los logs** (el JWT se registra completo en cada petición), y el repositorio incluye secretos por defecto.
3. **Borrar una tarea o un grupo con historial falla con 500**, por claves foráneas, y se pueden crear tareas sin título, porque no hay validación de entrada.

## Índice de hallazgos

| ID | Sev. | Área | Título | Verificación | Estado |
|---|---|---|---|---|---|
| [SEC-01](#sec-01) | Crítica | Backend · seguridad | Lecturas sin control de pertenencia al grupo (IDOR) | Reproducido | Resuelto |
| [SEC-02](#sec-02) | Crítica | Backend · seguridad | Cualquier usuario puede borrar requests ajenas | Reproducido | Resuelto |
| [SEC-03](#sec-03) | Alta | Backend · seguridad | El JWT completo se escribe en los logs | Reproducido | Resuelto |
| [SEC-04](#sec-04) | Alta | Config · seguridad | Secretos por defecto versionados (JWT y BD) | Código | Resuelto (falta HUM-01) |
| [DEP-01](#dep-01) | Alta | Frontend · dependencias | 5 vulnerabilidades npm (2 críticas, 3 altas) | `npm audit` | Resuelto |
| [BUG-01](#bug-01) | Alta | Backend · tasks/requests | Borrar una tarea con requests → 500 | Reproducido | Resuelto |
| [BUG-02](#bug-02) | Alta | Backend · groups | Borrar un grupo con tareas → 500 | Reproducido | Resuelto (soft delete, HUM-03) |
| [BUG-03](#bug-03) | Alta | Backend · tasks | Se crean tareas sin título (null persistido) | Reproducido | Resuelto |
| [OPS-01](#ops-01) | Alta | CI/CD | El deploy del backend no espera a los tests | Código | Resuelto |
| [SEC-05](#sec-05) | Media | Backend · iam | El registro permite autoasignarse `ROLE_ADMIN` | Código | Resuelto |
| [SEC-06](#sec-06) | Media | Backend · iam | `/users` expone a todos los usuarios con su email | Reproducido | Resuelto (email visible por HUM-06) |
| [SEC-07](#sec-07) | Media | Backend + frontend | No hay validación de entrada (Bean Validation) | Reproducido | Resuelto (frontend: FE-03) |
| [SEC-08](#sec-08) | Media | Backend · seguridad | CORS `*`, `@CrossOrigin` duplicado y Swagger público | Código | Resuelto |
| [SEC-09](#sec-09) | Media | Backend · seguridad | `TestController` activo: nunca se activa el perfil `prod` | Código | Resuelto (ECS: HUM-02) |
| [SEC-10](#sec-10) | Media | Frontend · sesión | Sesión: el 401 no cierra la sesión; token de 7 días en `localStorage` | Código | Resuelto (cookie HttpOnly, HUM-04) |
| [BUG-04](#bug-04) | Media | Backend · iam | Email duplicado → 500 (la validación compara el username) | Reproducido | Resuelto |
| [BUG-05](#bug-05) | Media | Backend · iam | Rol inexistente en el registro → 500 | Reproducido | Resuelto (por SEC-05) |
| [BUG-06](#bug-06) | Media | Backend · groups | Grupo sin `name` → 500 | Reproducido | Resuelto (por SEC-07) |
| [BUG-07](#bug-07) | Media | Backend · shared | El handler de 500 no registra la excepción | Código | Resuelto |
| [BUG-08](#bug-08) | Media | Backend · metrics | Las métricas "por usuario en grupo" ignoran `groupId` | Código | Resuelto |
| [BUG-09](#bug-09) | Media | Backend · tasks | Scheduler: duplicados con varias instancias, `System.out` y comentario incorrecto | Código | Resuelto |
| [ARC-01](#arc-01) | Media | Backend · arquitectura | Acceso directo a repositorios de otros contextos | Código | Pendiente |
| [ARC-04](#arc-04) | Media | Backend · persistencia | `ddl-auto=update` sin migraciones, OSIV activo y `show-sql` | Código | Resuelto |
| [PERF-01](#perf-01) | Media | Backend · rendimiento | Consultas N+1 y filtrado en memoria | Código | Resuelto |
| [OPS-02](#ops-02) | Media | Docker | Credenciales por defecto, sin perfil y sin `.env.example` | Código | Resuelto |
| [TEST-01](#test-01) | Media | Pruebas | Sin pruebas de autorización ni de iam/groups/invitations | Código | Resuelto (ver [TESTS.md](../TESTS.md)) |
| [SEC-11](#sec-11) | Baja | Seguridad | Sin límite de intentos de login | Código | Resuelto (Docker; AWS: HUM-02) |
| [SEC-12](#sec-12) | Baja | Docker · nginx | Faltan cabeceras de seguridad HTTP | Código | Resuelto |
| [DEP-02](#dep-02) | Baja | Backend · dependencias | Versiones de JJWT desalineadas | Código | Resuelto (0.12.7) |
| [DEP-03](#dep-03) | Baja | Dependencias | Sin escaneo automático de dependencias | Código | Resuelto |
| [BUG-10](#bug-10) | Baja | Backend · groups | `LeaderCannotBeRemovedException` responde 500 | Código | Resuelto |
| [BUG-11](#bug-11) | Baja | Backend · groups | `findByLeaderId` derivado de una propiedad inexistente | Código | Resuelto |
| [BUG-12](#bug-12) | Baja | Backend · groups | `GroupProfile.tasks` es `null` en los constructores | Código | Resuelto |
| [BUG-13](#bug-13) | Baja | Backend · iam | `@Data` en la entidad JPA `Role` | Código | Resuelto |
| [BUG-14](#bug-14) | Baja | Backend · shared | `ErrorResponse.timestamp` sin zona horaria | Reproducido | Resuelto |
| [BUG-15](#bug-15) | Baja | Frontend · iam | `Profile.userId` siempre `undefined` | Código | Resuelto |
| [BUG-16](#bug-16) | Baja | API · tasks | `UpdateTaskResource.requesterId` se ignora | Código | Resuelto |
| [BUG-17](#bug-17) | Baja | Frontend · groups | Búsqueda por código comentada aunque el backend la expone | Código | Resuelto (unirse por código, HUM-05) |
| [ARC-02](#arc-02) | Baja | Backend · estructura | Erratas de paquetes y capas mal ubicadas | Código | Pendiente |
| [ARC-03](#arc-03) | Baja | Backend + frontend | Código muerto o incompleto | Código | Pendiente |
| [PERF-02](#perf-02) | Baja | API | Listas sin paginación | Código | Resuelto |
| [PERF-03](#perf-03) | Baja | Frontend · tasks | "Mis tareas" hace una petición por grupo | Código | Resuelto |
| [FE-01](#fe-01) | Baja | Frontend | `console.log` y `console.error` en producción | Código | Resuelto |
| [FE-02](#fe-02) | Baja | Frontend | Mensajes de error mezclados en inglés y español | Código | Resuelto (traducción centralizada en `ToastStore`) |
| [FE-03](#fe-03) | Baja | Frontend · iam | El registro no exige una contraseña mínima | Código | Resuelto |
| [FE-04](#fe-04) | Baja | Repositorio | Archivos sobrantes (README genérico, `.vscode`, `package-lock` en el backend) | Código | Resuelto |
| [OPS-03](#ops-03) | Baja | CI/CD | Sin lint ni formato en CI | Código | Pendiente |
| [TEST-02](#test-02) | Baja | Pruebas | El test de contexto depende de un PostgreSQL local | Código | Resuelto |

Rutas abreviadas: `B/` = `backend/src/main/java/com/hub/synhub_backend/` y `F/` = `frontend/src/app/`.

---

## Seguridad

### SEC-01

**Lecturas sin control de pertenencia al grupo (IDOR)** · Crítica · Reproducido

**Evidencia.** Ninguno de estos endpoints verifica que el solicitante sea integrante (o líder) del grupo:

| Endpoint | Ubicación | Reproducción (usuario ajeno al grupo) |
|---|---|---|
| `GET /groups/{id}/members` | `B/groups/interfaces/rest/GroupController.java:74` | **200** |
| `GET /groups/{id}` | `GroupController.java:64` | (sin probar; mismo patrón) |
| `GET /invitations/group?groupId=` | `B/groups/interfaces/rest/InvitationController.java:94` | **200** |
| `GET /tasks/{id}` | `B/tasks/interfaces/rest/TaskController.java:66` | **200** |
| `GET /tasks/group/{groupId}` | `TaskController.java:76` | **200** |
| `GET /tasks/group/{g}/user/{u}` | `TaskController.java:85` | (mismo patrón) |
| `GET /tasks/{taskId}/requests` | `B/requests/interfaces/rest/RequestController.java:63` | **200** |
| `GET /tasks/{taskId}/requests/{id}` | `RequestController.java` | (mismo patrón) |

**Impacto.** Como el registro es abierto, cualquiera puede enumerar ids y leer las tareas, entregas, integrantes y emails de todos los grupos.

**Solución propuesta.**

1. Agregar a la ACL de groups (`GroupsContextFacade`) los métodos `boolean isMember(Long groupId, Long userId)` y `boolean isLeader(Long groupId, Long userId)`, implementados con `GroupProfileRepository.existsByGroup_IdAndUser_Id` y el rol.
2. Hacer que las queries afectadas reciban el `requesterId`: `GetTaskByIdQuery`, `GetTasksByGroupIdAndStatus`, `GetTasksByGroupIdAndUserIdQuery`, `GetGroupMembersByGroupIdQuery`, `GetGroupByIdQuery`, `GetInvitationsByGroupIdQuery`, `GetRequestsByTaskIdQuery` y `GetRequestByIdQuery`. El query service valida la pertenencia y lanza `AccessDeniedException` (403).
3. `GET /invitations/group`: solo el líder.
4. Para tareas y requests, derivar el grupo de `task.profile.group`.

**Criterios de aceptación.**

- Test de integración (MockMvc o REST) con los usuarios líder, miembro y ajeno: el ajeno recibe **403** en todos los endpoints de la tabla; el miembro recibe 200 en las lecturas del grupo y 403 en `/invitations/group`.
- Los tests existentes siguen en verde.
- La web sigue funcionando: todas las vistas consultan solo grupos propios.

### SEC-02

**Cualquier usuario puede borrar requests ajenas** · Crítica · Reproducido

**Evidencia.** `B/requests/interfaces/rest/RequestController.java:109` (`deleteRequestById`) construye `DeleteRequestCommand(requestId)` sin solicitante. Además, `RequestCommandServiceImpl.handle(DeleteRequestCommand)` no valida permisos ni que la request pertenezca a `taskId`. Reproducción: un usuario ajeno ejecuta `DELETE /tasks/{t}/requests/{r}` → **204**.

**Impacto.** Cualquiera puede borrar entregas pendientes (sabotear validaciones) o el historial de vencimientos.

**Solución propuesta.**

- Agregar `requesterId` y `taskId` a `DeleteRequestCommand`.
- Reglas: la request debe pertenecer a `taskId` (si no, 404). La puede borrar el **líder** del grupo, o el **autor** mientras siga `PENDING`. Las requests `EXPIRED` solo las borra el líder.
- La web no usa este endpoint. Alternativa válida: **eliminarlo** (opción recomendada si no hay un caso de uso).

**Criterios de aceptación.** Ajeno → 403. Miembro sobre una request revisada → 403 o 409. Líder → 204. `taskId` que no coincide → 404.

### SEC-03

**El JWT completo se escribe en los logs** · Alta · Reproducido

**Evidencia.**

- `B/iam/infrastructure/authorization/sfs/pipeline/BearerAuthorizationRequestFilter.java:55`: `LOGGER.info("Token: {}", token)`. Durante la reproducción hubo **22 tokens completos** en `docker logs`.
- Además, `TokenServiceImpl.java:98` registra "Token is valid" en INFO en cada petición.

**Impacto.** Cualquiera con acceso a los logs (CloudWatch, consola) puede suplantar usuarios durante 7 días.

**Solución propuesta.** Quitar el log del token. Bajar "Token is valid" y "Token is not valid" a `DEBUG`, sin incluir el token. Revisar que ningún otro log incluya headers ni contraseñas.

**Criterios de aceptación.** `grep -rn 'Token: {}' backend/src` no devuelve nada. Tras un login y una petición autenticada, el log no contiene `eyJ`.

### SEC-04

**Secretos por defecto versionados** · Alta · Código

**Evidencia.** `backend/src/main/resources/application.properties`:

- Líneas 20-21: `authorization.jwt.secret=${JWT_SECRET:<valor por defecto>}`.
- Línea 6: contraseña de BD por defecto.

El archivo figura en `backend/.gitignore`, pero **está versionado**. Si un entorno no define `JWT_SECRET`, usa un secreto público y cualquiera puede **firmar tokens válidos**.

**Solución propuesta.**

1. Quitar los valores por defecto del secreto JWT y de la contraseña: `${JWT_SECRET}`. La app debe fallar al arrancar si falta (por ejemplo, validando en `TokenServiceImpl` con `@PostConstruct` una longitud de al menos 32 bytes).
2. Crear `application-dev.properties` con los valores locales, solo para el perfil `dev`, y documentar `SPRING_PROFILES_ACTIVE=dev` para el desarrollo local.
3. Quitar `src/main/resources/application.properties` de `backend/.gitignore`: el archivo sí debe versionarse, pero sin secretos.
4. Actualizar `docker-compose.yml`, CI (`backend-ci.yml`: definir `JWT_SECRET` de prueba) y `docs/architecture/deployment.md`.

**Criterios de aceptación.** Sin `JWT_SECRET`, la app no arranca y el mensaje es claro. CI y compose siguen funcionando. **Ver [HUM-01](#decisiones-humanas-no-automatizar): rotar los secretos de los entornos reales.**

### SEC-05

**El registro permite autoasignarse `ROLE_ADMIN`** · Media · Código

**Evidencia.** `B/iam/application/internal/commandservices/UserCommandServiceImpl.java:77` toma los roles de `command.roles()`, que vienen de `SignUpResource.accountRoles`, enviado por el cliente. Hoy `ROLE_ADMIN` no habilita nada, pero es una escalada de privilegios latente.

**Solución propuesta.** En el registro público, ignorar `accountRoles` y asignar siempre `ROLE_USER`. Quitar `accountRoles` de `SignUpResource` y del frontend (`F/iam/presentation/views/sign-up-page/sign-up-page.ts:59`). Si se necesitan administradores, crearlos con un seed o un endpoint protegido.

**Criterios de aceptación.** Un registro con `["ROLE_ADMIN"]` devuelve un usuario con solo `ROLE_USER`. Esto también resuelve [BUG-05](#bug-05).

### SEC-06

**`/users` expone a todos los usuarios con su email** · Media · Reproducido

**Evidencia.** `B/iam/interfaces/rest/UsersController.java:45` (`GET /users`) devuelve 200 a un usuario cualquiera, con el email de todos. `GET /users/{id}` y `GET /users/username` también devuelven el email.

**Solución propuesta.**

- `GET /users`: restringirlo a `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`; `@EnableMethodSecurity` ya está activo) o eliminarlo, porque la web no lo usa.
- Crear un `PublicUserResource { id, username, name, surname, imgUrl }` para `/users/{id}`, `/users/username` y los recursos anidados (`TaskResource.assignedTo`, `InvitationResource.user`, miembros). Solo `/users/me` devuelve el email.

**Criterios de aceptación.** `GET /users` con `ROLE_USER` → 403. Ninguna respuesta incluye el email de otro usuario. La web sigue mostrando nombres y avatares.

**Resolución.** Por la decisión HUM-06, el email **sí** es visible para los demás. Se creó `PublicUserResource { id, username, name, surname, imgUrl, email }` para `/users/{id}`, `/users/username` y los usuarios anidados; solo `/users/me` incluye `accountRoles`. `GET /users` requiere `ROLE_ADMIN`.

### SEC-07

**No hay validación de entrada** · Media · Reproducido

**Evidencia.** Hay **0** usos de `@Valid` en el backend y ningún `@NotBlank`/`@Email`/`@Size` en `interfaces/rest/resources`. Reproducción:

- Un registro con contraseña `"1"` y email `"no-es-email"` → **201**.
- `difficulty: 9` en `POST /tasks` → **200**.
- Un enum inválido en un parámetro (`PUT /tasks/{id}/status?status=ARCHIVED`) → **500**: `MethodArgumentTypeMismatchException` cae en el handler genérico.

Hay pruebas que lo cubren en `api/ValidationApiTests`.

Al corregirlo aparecieron otros cinco 500 con la misma causa, ya resueltos: registro sin imagen (ahora usa `/default-avatar.jpg`), grupo sin descripción, edición de grupo con campos ausentes, JSON mal formado y parámetro obligatorio ausente.

**Solución propuesta.**

1. Anotar los `*Resource` de entrada:
   - `SignUpResource`: `@NotBlank` en todos los campos, `@Email`, `@Size(min=8)` en la contraseña y `@Size(max=50)` en `username`, `name` y `surname`.
   - `CreateGroupResource.name` y `CreateTaskResource.title`, `dueDate`, `userId`, `groupId`: obligatorios.
   - `CreateRequestResource.requestType`: obligatorio.
   - `difficulty`: `@Min(1) @Max(5)`.
2. Agregar `@Valid` en cada `@RequestBody`.
3. En `GlobalExceptionHandler`, manejar `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException` y `MethodArgumentTypeMismatchException` → **400**, con un mensaje que liste los campos.
4. Frontend: agregar validadores equivalentes (ver [FE-03](#fe-03)).

**Criterios de aceptación.** Los casos de [BUG-03](#bug-03), [BUG-06](#bug-06) y el registro débil responden **400** con el campo en el mensaje. Hay tests de controlador para cada resource.

### SEC-08

**CORS abierto, `@CrossOrigin` duplicado y Swagger público** · Media · Código

**Evidencia.**

- `B/iam/infrastructure/authorization/sfs/configuration/WebSecurityConfiguration.java:94`: `setAllowedOrigins(List.of("*"))`.
- `B/requests/interfaces/rest/RequestController.java:30`: `@CrossOrigin(origins="*")`.
- `/swagger-ui/**` y `/v3/api-docs/**` son públicos en todos los entornos.

**Solución propuesta.**

- Leer los orígenes permitidos de una propiedad: `app.cors.allowed-origins`, por defecto `http://localhost:4200`. En producción el front y la API comparten origen, así que puede quedar vacío.
- Eliminar el `@CrossOrigin`.
- Desactivar Swagger fuera de `dev`: `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false` en el perfil `prod`.

**Criterios de aceptación.** Una petición con `Origin: https://evil.example` no recibe `Access-Control-Allow-Origin`. Con el perfil `prod`, `/swagger-ui.html` → 404.

**Resolución.** CORS lee `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`), con credenciales para la cookie de sesión (HUM-04). Si la lista está vacía (producción, mismo origen), **no se procesa CORS**: la prueba en Docker mostró que, con una configuración vacía, Spring rechazaba el login con 403 porque nginx reenviaba el Host sin puerto y el `Origin` del navegador no coincidía. Además nginx reenvía ahora `$http_host` (`api/SameOriginApiTests`). Swagger está apagado en la configuración base y solo lo enciende el perfil `dev`.

### SEC-09

**`TestController` activo en todos los despliegues** · Media · Código

**Evidencia.** `B/tasks/interfaces/rest/TestController.java` usa `@Profile("!prod")`, pero ningún archivo del repo define `SPRING_PROFILES_ACTIVE` (ni el Dockerfile, ni compose, ni los workflows). Por lo tanto `/api/v1/tests/token` (emite tokens nuevos) y `/tests/fetch-user` existen en cualquier despliegue.

**Solución propuesta.** **Eliminar `TestController`** (recomendado: no tiene uso). Además, definir `SPRING_PROFILES_ACTIVE=prod` en el `Dockerfile` raíz, en `backend/Dockerfile` y en compose (ver [HUM-02](#decisiones-humanas-no-automatizar) para ECS).

**Criterios de aceptación.** `/api/v1/tests/*` → 404 o 401 en el contenedor.

**Resolución.** `TestController` eliminado; `SPRING_PROFILES_ACTIVE=prod` en ambos Dockerfile y en compose. Al probarlo apareció otro 500: cualquier ruta inexistente (`NoResourceFoundException`) o método no soportado caía en el handler genérico. Ahora responden 404, 405 o 415 (`api/HardeningApiTests`).

### SEC-10

**Gestión de sesión en el frontend** · Media · Código

**Evidencia.**

- `F/iam/infrastructure/iam.guard.ts:10` solo comprueba que haya un token en `localStorage`.
- Cuando el backend responde 401 (token expirado o inválido), `F/shared/application/toast.store.ts:65` muestra "Tu sesión expiró", pero **no cierra la sesión ni redirige**: el usuario queda en una app que falla en cada petición.
- El token dura 7 días (`application.properties:22`), no hay refresh ni revocación, y vive en `localStorage`, accesible para cualquier XSS.

**Solución propuesta.**

1. Cambiar `iamInterceptor` para que, ante un 401 en una ruta que no sea de autenticación, llame a `IamStore.signOut(router)` (o a un `clearSession` público) y redirija a `/auth/sign-in?expired=1`.
2. Hacer que el guard decodifique el `exp` del JWT (sin librerías, con `atob` del payload) y trate como no autenticado un token vencido.
3. Bajar la expiración a 1 día. Ver [HUM-04](#decisiones-humanas-no-automatizar) para la estrategia definitiva (refresh token o cookie `HttpOnly`).

**Criterios de aceptación.** Con un token vencido, al navegar a `/home` se redirige al login. Un 401 en cualquier petición limpia `localStorage` y redirige. Hay specs del interceptor y del guard.

**Resolución (HUM-04: cookie `HttpOnly` de 1 día).**

- Backend: el login responde `Set-Cookie: SYNHUB_SESSION=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=86400` y ya no devuelve el token en el cuerpo, sino `expiresAt`. Nuevo `POST /authentication/sign-out` que borra la cookie. El filtro acepta la cookie o el header `Bearer` (clientes de API). La expiración del JWT pasó de 7 días a 1.
- Frontend: el JWT no se guarda en ningún lado; `localStorage` solo tiene `userId` y `sessionExpiresAt`. El interceptor envía `withCredentials` a la API y ante un 401 cierra la sesión y lleva a `/auth/sign-in?expired=1`. El guard rechaza una sesión vencida. `signOut` llama al backend.
- CSRF: la cookie es `SameSite=Strict` y CORS solo admite los orígenes configurados (SEC-08).
- Despliegue: CloudFront debe reenviar la cookie en `/api/*` (HUM-02).

### SEC-11

**Sin límite de intentos de login** · Baja · Código

**Solución propuesta.** Aplicar `limit_req` en nginx para `/api/v1/authentication/` (por ejemplo, 5 r/min por IP con `burst=10`) en `docker/nginx.conf`. En AWS, usar una regla equivalente de WAF ([HUM-02](#decisiones-humanas-no-automatizar)). Alternativa en la app: Bucket4j.

**Criterios de aceptación.** El intento número 11 en un minuto desde la misma IP → 429.

**Resolución.** `docker/nginx.conf` limita `sign-in` y `sign-up` a 5 por minuto por IP con ráfaga de 10 (`limit_req_status 429`). Verificado con `docker compose`: tras 11 peticiones seguidas, la siguiente responde 429. En AWS el tráfico no pasa por este nginx: hace falta una regla de WAF (HUM-02).

### SEC-12

**Faltan cabeceras de seguridad en nginx** · Baja · Código

**Evidencia.** `docker/nginx.conf` solo agrega `Cache-Control`.

**Solución propuesta.** Agregar `X-Content-Type-Options nosniff`, `X-Frame-Options DENY`, `Referrer-Policy strict-origin-when-cross-origin` y una `Content-Security-Policy` compatible con Angular (`default-src 'self'; img-src 'self' https: data:; style-src 'self' 'unsafe-inline'; connect-src 'self'`). Usar `always` y repetirlas en el `location` que ya define `add_header`, porque nginx no las hereda.

**Criterios de aceptación.** `curl -I http://localhost:8080/` muestra las cuatro cabeceras y la web carga sin errores de CSP en la consola.

**Resolución.** Las cuatro cabeceras, con `always`, más `server_tokens off`. La CSP agrega Google Fonts (`fonts.googleapis.com`, `fonts.gstatic.com`), `object-src 'none'`, `base-uri 'self'` y `frame-ancestors 'none'`. El build de producción desactiva `inlineCritical` (`angular.json`): ese modo inserta un `<script>` en línea que `script-src 'self'` bloquearía y dejaría la página sin estilos. Verificado en el navegador con `docker compose`: login, recarga y cierre de sesión sin violaciones de CSP.

---

## Dependencias

### DEP-01

**Vulnerabilidades npm** · Alta · `npm audit`

**Evidencia** (`frontend/`, 2026-10-13):

| Paquete | Severidad | Tipo |
|---|---|---|
| `@angular/build` (< 22.2.0) | **Crítica** | directa (dev) |
| `piscina` 5.0.0–5.3.1 | **Crítica** | transitiva |
| `@angular/cli` (< 22.2.0) | Alta | directa (dev) |
| `@modelcontextprotocol/sdk` 1.12.0–1.30.1 | Alta | transitiva |
| `source-map-js` 1.0.0–1.2.1 | Alta | transitiva (también en el árbol de producción) |

**Solución propuesta.** Actualizar `@angular/build`, `@angular/cli` y los paquetes `@angular/*` a la última 22.x estable (≥ 22.2.0), con versiones alineadas. Después, `npm audit fix` sin `--force`, y verificar `ng test` y `ng build`.

**Criterios de aceptación.** `npm audit` reporta 0 vulnerabilidades críticas y altas. Build y tests en verde.

### DEP-02

**JJWT desalineado** · Baja

**Evidencia.** `backend/pom.xml`: `jjwt-api` y `jjwt-jackson` en 0.12.3, `jjwt-impl` en 0.12.6.

**Solución propuesta.** Definir la propiedad `<jjwt.version>` con la última 0.12.x y usarla en los tres artefactos.

**Criterios de aceptación.** Las tres versiones son iguales y `./mvnw verify` pasa.

### DEP-03

**Sin escaneo automático de dependencias** · Baja

**Solución propuesta.** Crear `.github/dependabot.yml` con los ecosistemas `npm` (`/frontend`), `maven` (`/backend`), `github-actions` (`/`) y `docker` (`/` y `/backend`), con frecuencia semanal. Agregar `npm audit --audit-level=high` al job de build del frontend.

**Criterios de aceptación.** El archivo existe y es válido. CI falla si aparece una vulnerabilidad alta o crítica.

---

## Defectos funcionales

### BUG-01

**Borrar una tarea con requests → 500** · Alta · Reproducido

**Evidencia.**

- `B/tasks/aplication/internal/commandservices/TaskCommandServiceImpl.java:107` borra la tarea sin borrar antes sus requests.
- Log: `update or delete on table "tasks" violates foreign key constraint … on table "requests"`.
- Ocurre siempre que la tarea tenga alguna entrega o vencimiento.

**Solución propuesta.**

1. Crear una ACL de requests (`RequestsContextFacade.deleteAllByTaskId(taskId)`) que use el `DeleteAllRequestsByTaskIdCommand` que ya existe.
2. Hacer que `TaskCommandServiceImpl.handle(DeleteTaskCommand)` lo invoque, en la misma transacción, antes de borrar la tarea.
3. Como `TaskStatusScheduler` ya depende de requests, conviene que **toda** la dependencia tasks → requests pase por esa fachada.
4. Mapear `DataIntegrityViolationException` → 409 en `GlobalExceptionHandler` como red de seguridad.

**Criterios de aceptación.** Borrar una tarea con requests → 204 y las requests desaparecen. Test de integración.

### BUG-02

**Borrar un grupo con tareas → 500** · Alta · Reproducido

**Evidencia.** `B/groups/application/internal/commandservices/GroupCommandServiceImpl.java:92` llama a `groupRepository.delete(group)`. La cascada `Group → GroupProfile → Task` (`cascade=ALL`) intenta borrar las tareas, y estas tienen requests (FK). Un grupo vacío sí se borra (204).

**Solución propuesta** (depende de [HUM-03](#decisiones-humanas-no-automatizar); por defecto, **borrado físico ordenado**):

- En `DeleteGroupCommand`, por cada tarea del grupo, borrar sus requests (fachada de [BUG-01](#bug-01)) y después las tareas (`TasksContextFacade`).
- Luego borrar las invitaciones y las membresías (por cascada) y el grupo, todo en una transacción.

**Criterios de aceptación.** Borrar un grupo con tareas y requests → 204, sin filas huérfanas. Test de integración.

### BUG-03

**Se crean tareas sin título** · Alta · Reproducido

**Evidencia.** `POST /tasks` sin `title` → **200**, y la tarea se guarda con `title = null`. Lombok `@NonNull` sobre los campos de `B/tasks/domain/model/aggregates/Task.java` **no** valida en el constructor manual `Task(CreateTaskCommand)` (línea 58), y la columna no es `NOT NULL`. Lo mismo ocurre con `description` y `dueDate`.

**Solución propuesta.**

- Validar en el resource ([SEC-07](#sec-07)).
- Validar también en el dominio: en `Task(CreateTaskCommand)` y en el `record CreateTaskCommand` (constructor compacto), lanzar `IllegalArgumentException` si `title` está en blanco o `dueDate` es null.
- Agregar `@Column(nullable = false)` en `title`, `description` y `dueDate`.
- Quitar los `@NonNull` de Lombok de los campos.

**Criterios de aceptación.** Sin `title` → 400. Tests unitarios del dominio y del controlador.

### BUG-04

**Email duplicado → 500** · Media · Reproducido

**Evidencia.** `B/iam/application/internal/commandservices/UserCommandServiceImpl.java:75` usa `existsByEmail(command.username())`, comparando con el username. Como el email repetido no se detecta, falla la restricción `unique` (`SQLState 23505`) y la respuesta es 500.

**Solución propuesta.** Cambiar la llamada a `existsByEmail(command.email())`. También mapear `DataIntegrityViolationException` → 409 (ver [BUG-01](#bug-01)).

**Criterios de aceptación.** Un email repetido → 409 con el mensaje `User with this email already exists`. Test unitario.

### BUG-05

**Rol inexistente en el registro → 500** · Media · Reproducido

**Evidencia.**

- `UserCommandServiceImpl.java:80` lanza `RuntimeException("Role name not found")`.
- Además, `Role.toRoleFromName` y el `enum` inválido en el JSON producen errores no mapeados.

**Solución propuesta.** Queda resuelto con [SEC-05](#sec-05), porque el registro deja de aceptar roles. Si se mantiene algún otro camino, lanzar `IllegalArgumentException` (400).

**Criterios de aceptación.** Un rol inválido nunca produce un 500.

### BUG-06

**Grupo sin `name` → 500** · Media · Reproducido

**Evidencia.** `POST /groups` sin `name` → 500 (Bean Validation falla en el `persist` con `@NotNull` de `Group`).

**Solución propuesta.** Agregar `@Valid` y `@NotBlank` en `CreateGroupResource` ([SEC-07](#sec-07)), y validar en `CreateGroupCommand` (constructor compacto). Mapear `ConstraintViolationException` → 400.

**Criterios de aceptación.** Sin `name` → 400.

### BUG-07

**El handler genérico no registra la excepción** · Media · Código

**Evidencia.** `B/shared/interfaces/rest/exceptions/GlobalExceptionHandler.java:74` (`handleGeneric`) devuelve 500 sin registrar `ex`. Durante la auditoría, los 500 solo fueron diagnosticables por los WARN de Hibernate. Además, el mensaje dice "ocurred" (errata).

**Solución propuesta.** Agregar un `Logger` y registrar con `log.error("Unhandled exception on {} {}", method, path, ex)`. Corregir la errata a "occurred". Opcional: un id de correlación (`MDC`) que se devuelva en `ErrorResponse`.

**Criterios de aceptación.** Un 500 provocado deja el stack trace en el log.

### BUG-08

**Las métricas "por usuario en grupo" ignoran `groupId`** · Media · Código

**Evidencia.** `B/metrics/application/internal/queryservice/TaskMetricsQueryServiceImpl.java`, líneas 36, 139, 152, 165 y 179, usan `findByProfile_User_Id`: mezclan las tareas de todos los grupos del usuario. Además, `GetTaskOverviewForUserInGroupQuery` y `GetTaskTimePassedForUserInGroupQuery` ni siquiera reciben `groupId`.

**Solución propuesta.** Agregar `groupId` a esas dos queries y usar `findByProfile_Group_IdAndProfile_User_Id(groupId, userId)` en todas las consultas "ForUserInGroup".

**Criterios de aceptación.** Tests unitarios: un usuario con tareas en dos grupos obtiene solo las del grupo pedido.

**Resolución.** Hecho junto con los endpoints base de metrics (HUM-05). Además, las métricas de una tarea respondían 400 (no existe) o 500 (estado no aplicable): ahora 404 y 409 (`MetricNotAvailableException`). Se quitó el `UserRepository.findAll()` que cargaba a todos los usuarios para obtener nombres.

### BUG-09

**Scheduler de vencimiento** · Media · Código

**Evidencia.** `B/tasks/domain/services/TaskStatusScheduler.java`:

- Línea 24: `fixedRate = 30000`, aunque el comentario dice "60 segundos".
- Línea 31: `System.out.println`.
- Todo el lote corre en una sola transacción.
- Con más de una instancia (ECS escalado), cada una procesa las mismas tareas y crea **requests `EXPIRED` duplicadas**.

**Solución propuesta.**

1. Usar un `Logger` (`DEBUG`).
2. Leer la frecuencia de una propiedad (`app.tasks.expiration-check-ms`, por defecto 60000) y corregir el comentario.
3. Evitar la duplicación con un bloqueo distribuido (ShedLock con JDBC, recomendado) o con una actualización condicional (`UPDATE … SET status='EXPIRED' WHERE id=? AND status='IN_PROGRESS'`) que cree la request solo si afectó una fila.
4. Mover el scheduler a `tasks/application/internal/schedulers/` ([ARC-02](#arc-02)).

**Criterios de aceptación.** Test: dos ejecuciones concurrentes sobre la misma tarea vencida crean **una** sola request `EXPIRED`.

**Resolución.** `UPDATE` condicional (`markExpiredIfInProgress`) en una transacción por tarea; solo crea la request quien cambió la fila. Logger en `DEBUG`, frecuencia en `app.tasks.expiration-check-ms` (60 s) y la clase movida a `tasks/aplication/internal/schedulers`. `api/TaskExpirationApiTests` corre dos ejecuciones a la vez contra PostgreSQL y verifica una sola request.

### BUG-10

**`LeaderCannotBeRemovedException` → 500** · Baja

**Evidencia.** `B/groups/domain/exceptions/LeaderCannotBeRemovedException.java:3` extiende `RuntimeException`.

**Solución propuesta.** Hacer que extienda `BusinessRuleViolationException` (409).

**Criterios de aceptación.** Test unitario del handler.

### BUG-11

**`findByLeaderId` derivado de una propiedad inexistente** · Baja

**Evidencia.** `B/groups/infrastructure/persistence/jpa/repositories/GroupRepository.java:14`: `Group` no tiene un campo `leader` (se calcula desde `userProfiles`). Solo lo usa `GetGroupsByLeaderIdQuery`, que nadie invoca.

**Solución propuesta.** Eliminar el método, la query y su handler. Alternativa: reimplementarlo con `GroupProfileRepository.findByUser_IdAndRoleInGroup(userId, GROUP_LEADER)`.

**Criterios de aceptación.** Compila y no queda ninguna referencia.

### BUG-12

**`GroupProfile.tasks` es `null`** · Baja

**Evidencia.** `B/groups/domain/model/entities/GroupProfile.java:36` asigna `this.tasks = null`, y el constructor con parámetros no lo inicializa. `addTask` lanzaría `NullPointerException` sobre una membresía recién creada en el mismo contexto de persistencia.

**Solución propuesta.** Declarar `private List<Task> tasks = new ArrayList<>();` y no reasignarla a `null`.

**Criterios de aceptación.** Test unitario: `new GroupProfile(u, g, r).addTask(t)` no lanza excepción.

### BUG-13

**`@Data` en la entidad `Role`** · Baja

**Evidencia.** `B/iam/domain/model/entities/Role.java:13`. `@Data` genera `equals`/`hashCode` sobre campos mutables, incluido el `id`, en una entidad que vive dentro de un `Set` (`User.accountRoles`).

**Solución propuesta.** Reemplazarlo por `@Getter`, `@Setter` y `@NoArgsConstructor`, con `equals`/`hashCode` basados en `name`, que es único.

**Criterios de aceptación.** Compila y los tests pasan.

### BUG-14

**`ErrorResponse.timestamp` sin zona** · Baja

**Evidencia.** `B/shared/interfaces/rest/exceptions/ErrorResponse.java:6` usa `LocalDateTime`. Las respuestas reproducidas muestran `"2026-10-13T07:45:23.16"` sin offset, mientras que el resto de la API usa `OffsetDateTime` UTC.

**Solución propuesta.** Usar `OffsetDateTime.now(ZoneOffset.UTC)`.

**Criterios de aceptación.** El timestamp termina en `Z` o en un offset.

### BUG-15

**`Profile.userId` siempre `undefined`** · Baja

**Evidencia.** `F/iam/infrastructure/profiles/profiles.assembler.ts:13` lee `resource.userId`, pero `UserResource` no lo envía.

**Solución propuesta.** Eliminar `userId` de `Profile` y de `ProfileResource` (el `id` ya es el del usuario), o mapearlo como `userId: resource.id`. Revisar los usos.

**Criterios de aceptación.** No queda ningún campo que siempre sea `undefined`. `ng test` pasa.

### BUG-16

**`UpdateTaskResource.requesterId` se ignora** · Baja

**Evidencia.** `B/tasks/interfaces/rest/resources/UpdateTaskResource.java:6`. El solicitante se toma del token, pero el contrato sugiere lo contrario y el frontend lo envía.

**Solución propuesta.** Quitar `requesterId` del resource, de `UpdateTaskRequest` y de `UpdateTaskCommand` del frontend.

**Criterios de aceptación.** El contrato ya no tiene el campo y la edición de tareas sigue funcionando.

### BUG-17

**Búsqueda de grupo por código comentada** · Baja

**Evidencia.** `F/groups/infrastructure/groups.api-endpoint.ts:54` tiene un TODO "reactivar cuando el backend exponga", pero `GET /groups/search?code=` ya existe.

**Solución propuesta.** Depende de [HUM-05](#decisiones-humanas-no-automatizar). Por defecto, **eliminar el código comentado** y actualizar el TODO, sin activar una función que no tiene flujo de unión asociado.

**Criterios de aceptación.** No queda código comentado que contradiga el backend.

**Resolución (HUM-05: terminar la función).** Nuevo `POST /groups/join { code }`: el código funciona como invitación abierta y el usuario entra como miembro. La web lo ofrece en "Mis grupos" (panel "Unirse a un grupo"). Se eliminaron el `searchByCode` y el `inviteUserToGroup` comentados.

---

## Arquitectura y persistencia

### ARC-01

**Acceso directo a repositorios de otros contextos** · Media · Código

**Evidencia** (ver [cross-feature-ownership.md](architecture/cross-feature-ownership.md#deuda-de-integración)):

- `TaskCommandServiceImpl` usa `GroupRepository`, `GroupProfileRepository` y `UserRepository`.
- `UserQueryServiceImpl` usa `GroupProfileRepository`.
- `TaskMetricsQueryServiceImpl` usa `TaskRepository` y `UserRepository`.
- `GroupCommandServiceImpl` e `InvitationCommandServiceImpl` usan `UserRepository`.

**Solución propuesta.** Ampliar las fachadas:

- `IamContextFacade.fetchUserById` (ya existe).
- `GroupsContextFacade.findMembership(groupId, userId)`, `isLeader` e `isMember` (ver [SEC-01](#sec-01)).
- `TasksContextFacade.findByGroupId(groupId)` para metrics.

Reemplazar las inyecciones directas. Mover `GetUsersByGroupIdQuery` a groups.

La relación JPA `Task.profile → GroupProfile` se **mantiene**: cambiarla por ids es una refactorización mayor que queda fuera de alcance.

**Criterios de aceptación.** `grep` de `…\.groups\.infrastructure` dentro de `tasks/`, `iam/` y `metrics/` (y equivalentes) sin resultados, salvo las entidades JPA. Los tests pasan.

### ARC-02

**Erratas de paquetes y capas mal ubicadas** · Baja

**Evidencia.** `tasks/aplication`, `metrics/infrastructure/persistenence`, `metrics/application/internal/queryservice`, `shared/domain/model/infrastructure/*`, `tasks/domain/services/TaskStatusScheduler` y `metrics/domain/model/services` (que devuelve DTOs REST).

**Solución propuesta.** Refactorizaciones puras, con un commit por renombre:

- `aplication` → `application`, `persistenence` → `persistence`, `queryservice` → `queryservices`.
- `shared/domain/model/infrastructure` → `shared/infrastructure`.
- Scheduler → `tasks/application/internal/schedulers`.
- `TaskMetricsQueryService` → `metrics/domain/services`, devolviendo value objects de dominio, con assemblers en `interfaces/rest/transform`.

Actualizar `application.properties` (FQCN de la estrategia de nombres) y la documentación.

**Criterios de aceptación.** Compila, los tests pasan y la documentación no menciona las rutas viejas.

### ARC-03

**Código muerto o incompleto** · Baja

**Evidencia.**

- Backend: `ExternalIamService`, `ExternalTasksService`, `RemoveMemberUserFromGroupCommand` (sin endpoint), `UpdateUserInfoCommand`, `GetUserByLeaderId`, `GetUserByMemberId`, `CreateUserLeaderCommandFromResourceAssembler`, `GetAllRequestsQuery`, `MetricsController` vacío y `InvitationRepository.findByUser_IdAndGroup_Id`.
- Frontend: `BaseApi` vacío, `IamStore.loadUsers` incompleto, `GroupsApi.getGroup`/`getGroupsByUser` sin uso e `inviteUserToGroup` comentado.

**Solución propuesta.** Eliminar todo lo que no tenga un uso planificado. Conservar `DeleteAllRequestsByTaskIdCommand`, que se usará en [BUG-01](#bug-01). Para `RemoveMemberUserFromGroupCommand` y `MetricsController`, ver [HUM-05](#decisiones-humanas-no-automatizar): por defecto, eliminar el controlador vacío y conservar el comando.

**Criterios de aceptación.** Compila, los tests pasan y la documentación de features queda actualizada.

### ARC-04

**Configuración de persistencia** · Media · Código

**Evidencia.** `application.properties`:

- Línea 13: `ddl-auto=update` (sin migraciones; los cambios destructivos o renombres no se aplican).
- Línea 14: `open-in-view=true` (oculta consultas lazy fuera de la transacción).
- Línea 10: `show-sql=true` (SQL en los logs de producción).

**Solución propuesta.**

1. Agregar **Flyway** con `V1__baseline.sql`, generado desde el esquema actual (`pg_dump --schema-only`), y `ddl-auto=validate`.
2. `show-sql=false` por defecto (activarlo solo en `dev`).
3. `open-in-view=false` **después** de [PERF-01](#perf-01), corrigiendo los `LazyInitializationException` con `@Transactional(readOnly = true)` en los query services o con fetch joins.

**Criterios de aceptación.** La app arranca contra una base vacía (migraciones) y contra una existente (baseline). Los tests de integración pasan con OSIV desactivado.

**Decisión (2026-10-13).** Se adopta Flyway. El entorno se volverá a desplegar desde cero, así que no hace falta *baseline* de una base existente: basta con `V1__init.sql` desde una base vacía.

**Resolución.** Flyway con `V1__init.sql`, generado desde las entidades (Hibernate 7) y revisado: restricciones con nombre e índices para las FK y para el scheduler. `ddl-auto=validate`, `open-in-view=false` y `show-sql=false` (activo en `dev`). Sin OSIV fallaban 30 pruebas por carga lazy: los command services pasaron a `@Transactional` y `Group.userProfiles` a EAGER (por lotes). `api/SchemaMigrationApiTests` lo verifica; además se probó en Docker sobre una base vacía recorriendo todos los endpoints de la web, sin errores en los logs.

### PERF-01

**Consultas N+1 y filtrado en memoria** · Media · Código

**Evidencia.**

- `GroupResource.memberCount` y `getLeader()` cargan `userProfiles` de cada grupo.
- `TaskResource` arma el grupo y el usuario de cada tarea (lazy).
- `RequestQueryServiceImpl` filtra por estado y tipo en memoria.
- `TaskMetricsQueryServiceImpl:101,153` usa `userRepository.findAll()` para buscar un nombre.

**Solución propuesta.**

- Usar `@EntityGraph` o `JOIN FETCH` en `findByProfile_Group_Id…` (profile, user y group).
- Crear una consulta derivada `findByTask_Profile_Group_IdAndRequestStatusAndRequestType`.
- Reemplazar `findAll()` por `findById` / `findAllById`.
- Marcar los query services con `@Transactional(readOnly = true)`.

**Criterios de aceptación.** Test con `spring.jpa.properties.hibernate.generate_statistics` (o un contador de sentencias): listar las tareas de un grupo de N tareas ejecuta un número constante de consultas.

**Resolución.** La medición confirmó el N+1 en cinco listados (por ejemplo, `GET /groups/user/role`: 7 sentencias con 1 grupo y 17 con 4). Con `hibernate.default_batch_fetch_size=100` los cinco quedan constantes (`api/QueryCountApiTests`). La bandeja de requests filtra y ordena en SQL (`findByGroupFiltered`), los query services son `@Transactional(readOnly = true)` y el `findAll()` de usuarios en metrics ya se había quitado con BUG-08.

### PERF-02

**Listas sin paginación** · Baja

**Solución propuesta.** Agregar `Pageable` opcional (`?page&size`, con un `size` máximo de 100) a `GET /tasks/group/{id}` y `GET /groups/{id}/requests`, conservando la respuesta en forma de arreglo cuando no se pide paginación, para no romper la web.

**Criterios de aceptación.** `?size=1` devuelve un elemento y sin parámetros el comportamiento no cambia.

**Decisión (2026-10-13).** Aprobada tal como se propone: paginación opcional; sin parámetros la respuesta sigue siendo un arreglo.

### PERF-03

**"Mis tareas" hace una petición por grupo** · Baja

**Evidencia.** `F/tasks/application/tasks.store.ts:100` (`loadTasksByGroupsAndUser`) y `loadAssignedGroupIds`.

**Solución propuesta.** Crear el endpoint `GET /tasks/user` (tareas del usuario del token en todos sus grupos; la query `GetAllTasksByUserId` ya existe) y usarlo en `MemberTasks`.

**Criterios de aceptación.** `/tasks/member` hace una sola petición de tareas.

---

## Frontend

### FE-01

**Logs en consola** · Baja

**Evidencia.**

- `F/iam/infrastructure/iam.guard.ts:9`: `console.log` en cada navegación.
- `F/shared/infrastructure/base-api-endpoint.ts:23`: `console.log(response)`.
- `F/iam/application/iam.store.ts`: tres `console.error`.

**Solución propuesta.** Eliminarlos. Los errores ya se muestran con `ToastStore`.

**Criterios de aceptación.** `grep -rn "console\." frontend/src/app --include=*.ts | grep -v spec` no devuelve nada.

### FE-02

**Mensajes de error mezclados** · Baja

**Evidencia.** Los `handleError('Failed to …')` están en inglés, y los mensajes de invitations y requests, en español.

**Solución propuesta.** Traducir los textos de `handleError` al español, o centralizar los mensajes en `ToastStore.toFriendlyMessage`.

**Criterios de aceptación.** Ningún mensaje que vea el usuario queda en inglés.

### FE-03

**El registro no exige una contraseña mínima** · Baja

**Evidencia.** `F/iam/presentation/views/sign-up-page/sign-up-page.ts:42`: la contraseña solo usa `Validators.required` (el email ya tiene `Validators.email`). Hay una prueba deshabilitada que lo cubre en `sign-up-page.spec.ts` (`AUDIT FE-03`).

**Solución propuesta.** Agregar `Validators.minLength(8)` a la contraseña, alineado con [SEC-07](#sec-07), con su mensaje en `BaseForm`.

**Criterios de aceptación.** Spec del formulario: una contraseña de 7 caracteres es inválida.

### FE-04

**Archivos sobrantes** · Baja

**Evidencia.**

- `frontend/README.md` es el texto genérico de Angular CLI.
- `frontend/.vscode/` está versionado.
- `backend/package-lock.json` sobra en un proyecto Maven.

**Solución propuesta.**

- Reemplazar `frontend/README.md` por un README breve que remita a `docs/features/*/frontend` y `docs/architecture/frontend.md`. Hacer lo mismo con un `backend/README.md`.
- Borrar `backend/package-lock.json`.
- Mantener `.vscode` solo si el equipo lo usa (por defecto, mantener `extensions.json` y borrar el resto).

**Criterios de aceptación.** Los archivos quedan limpios y los enlaces de la documentación siguen válidos (ver el script de verificación de enlaces del [anexo](#anexo-scripts-de-verificación)).

---

## Operaciones y CI/CD

### OPS-01

**El deploy del backend no espera a los tests** · Alta · Código

**Evidencia.** `.github/workflows/backend-deploy.yml` corre en el push a `main` en paralelo con `backend-ci.yml`, compila con `-DskipTests` y no tiene `needs` ni `workflow_run`. Un commit con los tests en rojo se despliega igual.

**Solución propuesta.** Unificarlos en un workflow `backend.yml` con un job `test` y un job `deploy` que tenga `needs: test` y `if: github.ref == 'refs/heads/main' && github.event_name == 'push'`, conservando los filtros `paths`. Hacer lo mismo con el frontend, que ya usa `needs: build`.

**Criterios de aceptación.** En un push a `main` con un test fallando, el job de deploy no se ejecuta.

### OPS-02

**Docker y compose con valores inseguros** · Media · Código

**Evidencia.**

- `docker-compose.yml:9` usa `DATABASE_PASSWORD:-postgres` por defecto.
- No hay `JWT_SECRET` (ver [SEC-04](#sec-04)) ni `SPRING_PROFILES_ACTIVE`.
- No existe `.env.example`.
- El proceso maestro de nginx corre como root.

**Solución propuesta.**

- Crear `.env.example` con todas las variables (sin valores reales) y agregar `.env` al `.gitignore`.
- En compose, usar `${JWT_SECRET:?defínelo en .env}` y `${DATABASE_PASSWORD:?…}`.
- Definir `SPRING_PROFILES_ACTIVE=prod` en el `Dockerfile` raíz.
- Documentar todo en `deployment.md`. (nginx como root para escuchar en el puerto 80 es aceptable; opcionalmente, escuchar en el 8080 sin root.)

**Criterios de aceptación.** `docker compose up` sin `.env` falla con un mensaje claro, y con `.env` funciona.

### OPS-03

**Sin lint ni formato en CI** · Baja

**Solución propuesta.**

- Frontend: `ng add @angular-eslint/schematics` y un paso `npx ng lint` en CI.
- Backend: Spotless (google-java-format o palantir) en modo `check` dentro de `verify`.
- Aplicar el formato en un commit aparte, solo de formato.

**Criterios de aceptación.** CI ejecuta lint y formato y falla si hay errores.

---

## Pruebas

### TEST-01

**Cobertura insuficiente en las zonas de riesgo** · Media · Código

**Evidencia.**

- Backend: solo hay tests en requests (23), tasks (1) y ai (3 + 1 IT). No hay ninguno para iam, groups, invitations, controladores ni seguridad.
- Frontend: sin specs de stores (salvo `AiStore`), del interceptor, del guard, de requests, de invitations ni de tour.

**Solución propuesta.**

- Backend: tests de integración con `@SpringBootTest` + MockMvc + Testcontainers (ver [TEST-02](#test-02)) que cubran la **matriz de autorización** de [SEC-01](#sec-01) y [SEC-02](#sec-02), el registro y el login (BUG-04, SEC-05, SEC-07), las invitaciones y el borrado en cascada (BUG-01, BUG-02).
- Frontend: specs de `IamStore`, `iamInterceptor`, `iamGuard`, `InvitationsStore`, `RequestsStore` y `TasksStore`, con `HttpTestingController`.

**Criterios de aceptación.** Cada hallazgo corregido de las fases 1 y 2 tiene al menos un test que fallaría sin la corrección.

### TEST-02

**El test de contexto depende de un PostgreSQL local** · Baja

**Evidencia.** `SynhubBackendApplicationTests` necesita `localhost:5432`, y en CI se resuelve con un servicio de GitHub Actions.

**Solución propuesta.** Agregar `spring-boot-testcontainers` y `org.testcontainers:postgresql` (scope `test`), con `@ServiceConnection` en una configuración de test compartida. Así, `./mvnw verify` funciona sin una base local; el servicio de CI se puede quitar o mantener.

**Criterios de aceptación.** `./mvnw verify` pasa en una máquina con Docker y sin PostgreSQL.

---

## Decisiones humanas (no automatizar)

| ID | Decisión / acción | Relacionado | Recomendación | Decisión tomada (2026-10-13) |
|---|---|---|---|---|
| HUM-01 | **Rotar** `JWT_SECRET` y las credenciales de BD en todos los entornos que hayan usado los valores por defecto. Cargar el secreto en SSM. | SEC-04 | Hacerlo antes de desplegar la fase 1. | Pendiente (requiere acceso a AWS). |
| HUM-02 | Configurar AWS: `SPRING_PROFILES_ACTIVE=prod` en la task definition de ECS, reenvío de cookies (sin caché) en el *behavior* `/api/*` de CloudFront (sesión HUM-04), la *trust policy* OIDC de `synhub_2`, las variables `AWS_DEPLOY_ROLE_ARN`/`AWS_ACCOUNT_ID` del repo y WAF o rate limit en CloudFront/ALB. | SEC-09, SEC-11, OPS-01 | — | Pendiente (requiere acceso a AWS y GitHub). |
| HUM-03 | Semántica del borrado de grupos y tareas: borrado físico o *soft delete* (archivar). | BUG-01, BUG-02 | Borrado físico ordenado ahora; evaluar *soft delete* si se necesita historial. | **Soft delete.** Grupos, tareas, requests y membresías borrados se marcan y dejan de ser visibles en toda la API; los datos quedan en la base. Implementado: `@SoftDelete` (`deleted_at`) en `Group`, `GroupProfile`, `Invitation`, `Task` y `Request` (`api/SoftDeleteApiTests`). |
| HUM-04 | Estrategia de sesión: token corto + refresh token, o cookie `HttpOnly`. | SEC-10 | Token de 1 día + logout ante un 401 ahora; cookie `HttpOnly` en una iteración posterior. | **Cookie `HttpOnly` válida por 1 día.** El JWT deja de guardarse en `localStorage`; un 401 cierra la sesión. |
| HUM-05 | Funciones a medio hacer: unión a un grupo por código, expulsar miembros (`RemoveMemberUserFromGroupCommand`) y endpoints de metrics. | BUG-17, ARC-03 | Eliminar lo que no esté en el roadmap. | **Terminarlas.** Unirse por código y expulsar miembros quedan completos (API y web); metrics recibe una implementación base de sus endpoints. Hecho: `POST /groups/join` y `DELETE /groups/{id}/members/{userId}` (API y web) y 11 endpoints `GET /metrics/...` con control de acceso. |
| HUM-06 | Privacidad: qué datos de un usuario pueden ver los demás (por ejemplo, el email dentro del grupo). | SEC-06 | Ocultar el email fuera de `/users/me`. | **Los demás ven nombre, correo y foto** (además de `id` y `username`). Roles y grupos ajenos no se exponen; `GET /users` queda solo para administradores. |

## Plan de ejecución

| Fase | Objetivo | Hallazgos | Notas |
|---|---|---|---|
| 0 | Base de pruebas | TEST-02 | Habilita los tests de integración de las fases siguientes. |
| 1 | Seguridad crítica | SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, DEP-01 | Escribir primero los tests de autorización (TEST-01, parte backend). SEC-01 requiere ampliar `GroupsContextFacade`. |
| 2 | Errores 500 y validación | SEC-07, BUG-03, BUG-04, BUG-05, BUG-06, BUG-07, BUG-01, BUG-02 | BUG-01 antes que BUG-02. SEC-07 cubre parte de BUG-03 y BUG-06. |
| 3 | Endurecimiento y CI | SEC-06, SEC-08, SEC-09, SEC-10, SEC-11, SEC-12, OPS-01, OPS-02, DEP-02, DEP-03 | SEC-10 incluye las specs del interceptor y del guard. |
| 4 | Correcciones menores y frontend | BUG-08 … BUG-17, FE-01 … FE-04, TEST-01 (frontend) | — |
| 5 | Rendimiento y persistencia | PERF-01, PERF-02, PERF-03, ARC-04 | ARC-04 (OSIV off) después de PERF-01. |
| 6 | Refactorización de arquitectura | ARC-01, ARC-02, ARC-03, OPS-03 | Commits de refactorización pura; el formato (OPS-03) va al final para no ensuciar los diffs. |
| 7 | Cierre | — | Actualizar `docs/` (secciones "Observaciones" y "Desviaciones conocidas"), poner este índice en `Resuelto` y volver a ejecutar el anexo. |

## Anexo: scripts de verificación

**Reproducción de seguridad y defectos.** Script Python usado en la auditoría, adaptable como test de humo. Con el stack levantado (`docker compose up`), registrar los usuarios `leader`, `member`, `outsider` y `xuser`, y ejecutar las peticiones de la tabla. Resultados al momento de la auditoría (esperados tras la corrección entre paréntesis):

| Caso | Obtenido | Esperado |
|---|---|---|
| Registro con email duplicado | 500 | 409 |
| Registro con contraseña `"1"` y email inválido | 201 | 400 |
| Registro con un rol inexistente | 500 | 201 con `ROLE_USER` (SEC-05) |
| `POST /groups` sin `name` | 500 | 400 |
| `POST /tasks` sin `title` | 200 | 400 |
| Ajeno: `GET /invitations/group?groupId=` | 200 | 403 |
| Ajeno: `GET /groups/{id}/members` | 200 | 403 |
| Ajeno: `GET /tasks/group/{id}` | 200 | 403 |
| Ajeno: `GET /tasks/{id}` | 200 | 403 |
| Ajeno: `GET /tasks/{id}/requests` | 200 | 403 |
| Ajeno: `GET /users` | 200 | 403 |
| Ajeno: `DELETE /tasks/{t}/requests/{r}` | 204 | 403 |
| Líder: `DELETE /tasks/{id}` con requests | 500 | 204 |
| Líder: `DELETE /groups/{id}` con tareas | 500 | 204 |
| Líder: `DELETE /groups/{id}` vacío | 204 | 204 |
| Token alterado | 401 | 401 |
| Tokens completos en `docker logs` | 22 | 0 |

**Enlaces de la documentación.** Comprobar que todos los enlaces Markdown relativos de los `.md` apunten a archivos existentes y que las anclas `#…` coincidan con encabezados (slug de GitHub: minúsculas, sin puntuación y espacios → `-`). Al momento de la auditoría: 238 enlaces, 0 rotos.
