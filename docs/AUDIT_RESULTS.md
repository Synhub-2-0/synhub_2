# Resultados de la auditoría de SynHub

Resumen de la auditoría técnica del monorepo y de su corrección. El detalle de cada hallazgo (evidencia, solución y criterios de aceptación) está en [AUDITORIA.md](AUDITORIA.md); el inventario de pruebas, en [TESTS.md](../TESTS.md).

| | |
|---|---|
| **Fecha** | 2026-10-13 |
| **Commit auditado** | `68536da` (rama `main`) |
| **Rama de corrección** | `audit`: 41 commits, en general uno por hallazgo |
| **Estado** | 49 hallazgos resueltos (48 de la auditoría + BUG-18, hallado durante la corrección) |
| **Pendiente** | Configuración de AWS y GitHub (HUM-01, HUM-02) y fusionar `audit` en `main` |

## 1. Resultado en cifras

| Indicador | Al auditar | Tras la corrección |
|---|---|---|
| Hallazgos abiertos | 48 (2 críticos, 7 altos, 17 medios, 22 bajos) | 0 |
| Pruebas del backend | 28 | 262 (unitarias, de API contra PostgreSQL real y de arquitectura) |
| Pruebas del frontend | 25 | 202 |
| Pruebas deshabilitadas que documentaban defectos | todas las del inventario | 0: habilitadas y pasando |
| Vulnerabilidades npm altas o críticas | 5 | 0 |
| Casos del anexo de reproducción con el resultado esperado | 2 de 17 | 18 de 18 |
| Tokens de sesión en los logs | 22 en una sesión de prueba | 0 |
| Lint y formato en CI | no | sí (ESLint y Spotless) |

Los casos del anexo se repiten con [`scripts/audit-smoke.py`](../scripts/audit-smoke.py) contra `docker compose`.

## 2. Qué se corrigió

### Seguridad

| ID | Antes | Ahora |
|---|---|---|
| SEC-01 (crítico) | Cualquier usuario leía grupos, tareas, entregas e invitaciones ajenos. | Solo los integrantes del grupo (invitaciones: solo el líder); el resto recibe 403. |
| SEC-02 (crítico) | Cualquier usuario borraba entregas ajenas. | El líder borra cualquiera; el asignado, solo las suyas pendientes. |
| SEC-03 | El JWT completo se escribía en los logs. | Nunca se registra. |
| SEC-04 | Secretos de JWT y base de datos por defecto en el repositorio. | Sin valores por defecto; sin ellos la app no arranca. Perfil `dev` y `.env.example` para local. |
| SEC-05 | El registro permitía asignarse `ROLE_ADMIN`. | Siempre `ROLE_USER`. |
| SEC-06 | `GET /users` exponía a todos los usuarios con su email. | Solo administradores. Los demás ven nombre, correo y foto, sin roles (HUM-06). |
| SEC-07 | Sin validación de entrada; datos inválidos daban 201 o 500. | Bean Validation: 400 con los campos que fallan. |
| SEC-08 | CORS `*`, Swagger público. | CORS solo para orígenes configurados; Swagger solo en `dev`. |
| SEC-09 | Endpoint de depuración que emitía tokens, activo en producción. | Eliminado; perfil `prod` en Docker. |
| SEC-10 | Token de 7 días en `localStorage`; un 401 no cerraba la sesión. | Cookie `HttpOnly` de 1 día (HUM-04); la web cierra la sesión ante un 401. |
| SEC-11 | Sin límite de intentos de login. | nginx: 5 por minuto por IP con ráfaga de 10, luego 429. |
| SEC-12 | Sin cabeceras de seguridad. | CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy`. |

### Defectos funcionales

| ID | Corrección |
|---|---|
| BUG-01, BUG-02 | Borrar tareas o grupos con datos relacionados respondía 500; ahora funciona (con borrado lógico, HUM-03). |
| BUG-03 | No se crean tareas sin título ni fecha límite. |
| BUG-04, BUG-05, BUG-06 | Email duplicado → 409; rol inexistente y grupo sin nombre ya no dan 500. |
| BUG-07 | Los 500 inesperados quedan registrados con su causa. |
| BUG-08 | Las métricas por integrante solo cuentan el grupo pedido. |
| BUG-09 | El vencimiento automático no duplica requests con varias instancias. |
| BUG-10 | Quitar al líder responde 409 en lugar de 500. |
| BUG-11 a BUG-16 | Consulta derivada inválida eliminada, `Role` sin `@Data`, fechas de error en UTC, `Profile.userId` correcto, `requesterId` fuera del contrato de edición. |
| BUG-17 | La búsqueda por código comentada se reemplazó por la función completa de unirse por código (HUM-05). |
| BUG-18 (nuevo) | La API permitía editar, mover o borrar una tarea `DONE`; ahora responde 409. |

Durante la corrección aparecieron y se resolvieron además: cinco casos más de 500 por datos de entrada (SEC-07), rutas inexistentes que respondían 500, un login rechazado con 403 detrás de nginx (regresión de SEC-08) y la expulsión de integrantes, que no borraba nada.

### Rendimiento y persistencia

| ID | Corrección |
|---|---|
| PERF-01 | Cinco listados tenían N+1 (por ejemplo, de 7 a 17 consultas con 4 grupos); ahora el número de consultas es constante y una prueba lo mide. |
| PERF-02 | Paginación opcional (`?page`, `?size` hasta 100); sin parámetros, la respuesta no cambia. |
| PERF-03 | "Mis tareas" hace una sola petición (`GET /tasks/user`). |
| ARC-04 | Flyway (`V1__init.sql`), `ddl-auto=validate`, sin open-in-view, sin SQL en los logs. |

### Arquitectura, frontend y operación

| ID | Corrección |
|---|---|
| ARC-01 | Los contextos solo se comunican por sus fachadas `*ContextFacade`; una prueba lo verifica. |
| ARC-02 | Erratas de paquetes corregidas y capas reubicadas; las métricas devuelven objetos de dominio. |
| ARC-03 | Código muerto eliminado en backend y frontend. |
| FE-01 a FE-04 | Sin `console.*`, mensajes de error en español, contraseña mínima en el registro, READMEs de módulo y archivos sobrantes eliminados. |
| OPS-01 | El deploy del backend depende de que pasen sus pruebas y despliega el mismo jar probado. |
| OPS-02 | Compose sin credenciales por defecto, `.env.example`, perfil `prod`. |
| OPS-03 | ESLint (con reglas de accesibilidad) y formato Java (Spotless) en CI. |
| DEP-01 a DEP-03 | Angular actualizado, JJWT alineado, Dependabot y `npm audit` en CI. |
| TEST-01, TEST-02 | Pruebas de autorización, de API y de dominio; PostgreSQL de prueba con Testcontainers. |

## 3. Decisiones de producto aplicadas

| ID | Decisión | Implementación |
|---|---|---|
| HUM-03 | Borrado lógico | Grupos, membresías, invitaciones, tareas y requests se marcan con `deleted_at` y dejan de verse en toda la API. |
| HUM-04 | Cookie `HttpOnly` de 1 día | `SYNHUB_SESSION` (`Secure`, `SameSite=Strict`, `Path=/api`), `POST /authentication/sign-out`. |
| HUM-05 | Terminar las funciones a medio hacer | Unirse por código (`POST /groups/join`), quitar integrantes o salir (`DELETE /groups/{id}/members/{userId}`) y 11 endpoints de métricas. |
| HUM-06 | Datos visibles de otros usuarios | Nombre, correo y foto. |
| PERF-02 | Paginación | Opcional, sin romper la web. |
| ARC-04 | Migraciones | Flyway desde una base vacía (el entorno se despliega desde cero). |

Decisiones tomadas por defecto durante la implementación (pueden revisarse): unirse con el código hace miembro de inmediato, sin aprobación; las métricas del grupo las ve cualquier integrante y las de una persona, ella y el líder; los clientes de API pueden seguir usando `Authorization: Bearer`, pero el login ya no devuelve el token en el cuerpo.

## 4. Cómo se verificó

- Cada hallazgo con defecto reproducible tiene una prueba que fallaba antes de corregirlo.
- `./mvnw -B verify` (pruebas y formato) y `npx ng lint`, `npx ng test --watch=false`, `npx ng build` después de cada fase.
- Pruebas en Docker con el navegador: login por cookie, recarga y cierre de sesión, CSP sin violaciones, límite de intentos, unirse por código y salir del grupo.
- Recorrido de todos los endpoints que usa la web sobre una base vacía (Flyway), sin errores en los logs.
- Anexo de reproducción: 18 de 18 casos con el resultado esperado.
- Enlaces de la documentación: 345 enlaces relativos, ninguno roto.

## 5. Pendiente

### Antes de desplegar en AWS (HUM-01, HUM-02)

Requieren acceso a AWS y a la configuración de GitHub; no se pueden hacer desde el repositorio.

- [ ] Generar `JWT_SECRET` (al menos 32 caracteres aleatorios) y `DATABASE_PASSWORD` nuevos y guardarlos en SSM. Sin ellos el backend no arranca.
- [ ] Task definition de ECS: `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` y `DATABASE_PASSWORD`.
- [ ] CloudFront, *behavior* `/api/*`: reenviar las cookies (al menos `SYNHUB_SESSION`) y no cachear. Sin esto, toda petición autenticada responde 401.
- [ ] Regla de WAF (CloudFront o ALB) que limite los intentos de login: en AWS el tráfico no pasa por el nginx del contenedor.
- [ ] Rol IAM con *trust policy* OIDC para `Synhub-2-0/synhub_2` y variables del repositorio `AWS_DEPLOY_ROLE_ARN` y `AWS_ACCOUNT_ID`.
- [ ] Desplegar sobre una base de datos vacía: Flyway crea el esquema. Una base creada con el esquema anterior no arranca.

### En el repositorio

- [ ] Abrir el pull request `audit` → `main` para que el CI completo (Testcontainers, formato, lint, `npm audit`) corra antes de fusionar.
- [ ] En entornos locales que ya usaron `docker compose`: `docker compose down -v` antes de levantar la nueva versión.

### Mejoras posibles (fuera del alcance de la auditoría)

- Vista de métricas en la web: la API base ya existe.
- Botón para eliminar un grupo en la web: el backend lo soporta.
- Desviaciones que se mantuvieron a propósito: relaciones JPA entre contextos (por ejemplo, `Task.profile → GroupProfile`), excepciones de negocio en `shared/interfaces/rest` y resolución del usuario de la sesión con `UserQueryService` en cada controlador. Ver [architecture/backend.md §10](architecture/backend.md#10-desviaciones-conocidas-respecto-a-la-estructura-canónica).
- Pruebas end-to-end en navegador (Playwright o Cypress): hoy los flujos se cubren por HTTP.
