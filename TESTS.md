# Inventario de pruebas

Registro de **todo lo que necesita casos de prueba** en el monorepo y del **estado** de cada archivo de pruebas. Si una sesión de trabajo se interrumpe, este documento indica qué está listo y qué falta.

## Cómo usarlo

- **Estados**:

  | Estado | Significado |
  |---|---|
  | `PENDIENTE` | Aún no existe el archivo, o le faltan casos de la columna "Casos". |
  | `EN CURSO` | Se estaba escribiendo cuando se detuvo la sesión: revisar el archivo antes de continuar. |
  | `LISTO` | El archivo existe y todos sus casos pasan. |
  | `LISTO-PARCIAL` | El archivo existe y pasa; algunos casos están deshabilitados porque documentan un defecto de [docs/AUDITORIA.md](docs/AUDITORIA.md) (se habilitan al corregirlo). |
  | `PREVIO` | Ya existía antes de este inventario; se revisó y pasa. |
  | `FALLA` | Existe pero falla: ver nota. |

- **Pruebas que documentan defectos**: se escriben con el comportamiento **esperado** y quedan deshabilitadas, con el ID de la auditoría:
  - Backend: `@Disabled("AUDIT <ID>: <motivo>")`
  - Frontend: `it.skip('AUDIT <ID>: …')`

  El prompt de corrección debe **habilitarlas** al resolver ese ID (son la prueba que falla primero). Para listarlas:
  ```bash
  grep -rn "AUDIT " backend/src/test frontend/src --include=*.java --include=*.spec.ts
  ```
- **Comandos**:
  ```bash
  cd backend && ./mvnw -B test                          # unitarias + API (requiere PostgreSQL)
  cd backend && ./mvnw -B test -Dtest='!*ApiTests'      # solo unitarias, sin BD
  cd frontend && npx ng test --watch=false
  ```
  Las pruebas `*ApiTests` (backend) levantan la app completa en un puerto aleatorio contra PostgreSQL (`DATABASE_URL`, `DATABASE_USERNAME` y `DATABASE_PASSWORD`; por defecto, `localhost:5432`).
- **Para retomar**: buscar la primera fila `PENDIENTE` o `EN CURSO` en el orden de las tablas, escribir el archivo, ejecutarlo y actualizar su fila y la [bitácora](#bitácora).

## Resumen

| Capa | Archivos | LISTO / PREVIO | LISTO-PARCIAL | PENDIENTE |
|---|---|---|---|---|
| Backend: pruebas unitarias | 26 | 19 | 7 | 0 |
| Backend: API (integración) | 6 | 2 | 4 | 0 |
| Frontend | 37 | 0 | 0 | 37 |
| **Total** | **69** | **21** | **11** | **37** |

---

## Backend: pruebas unitarias

JUnit 5 + Mockito, sin Spring ni base de datos. Ruta base: `backend/src/test/java/com/hub/synhub_backend/`.

| ID | Archivo | Objetivo | Casos | Auditoría | Estado |
|---|---|---|---|---|---|
| B-IAM-01 | `iam/UserCommandServiceImplTest.java` | `UserCommandServiceImpl` | sign-in correcto devuelve usuario+token · contraseña incorrecta → `BadCredentialsException` · usuario inexistente → `BadCredentialsException` · sign-up guarda con hash y roles resueltos · username duplicado → 409 · rol inexistente · email duplicado → 409 | BUG-04, BUG-05 | LISTO-PARCIAL |
| B-IAM-02 | `iam/RoleTest.java` | `Role`, `RoleCommandServiceImpl` | lista vacía o null → `ROLE_USER` · `toRoleFromName` válido e inválido · el seed crea solo los roles faltantes | — | LISTO |
| B-IAM-03 | `iam/TokenServiceImplTest.java` | `TokenServiceImpl` | genera un token con el username como subject · valida un token propio · rechaza un token alterado · rechaza un token firmado con otra clave · rechaza un token expirado · extrae el bearer del header (presente, ausente, sin prefijo) | — | LISTO |
| B-IAM-04 | `iam/UserQueryServiceImplTest.java` | `UserQueryServiceImpl` | por id, por username, todos · usuarios de un grupo a partir de las membresías | — | LISTO |
| B-IAM-05 | `iam/IamContextFacadeImplTest.java` | `IamContextFacadeImpl` | `fetchUserIdByUsername` (encontrado → id, no encontrado → 0) · `fetchUsernameByUserId` (vacío si no existe) · `fetchUserById` con id null o ≤ 0 → vacío · `createUser` con rol por defecto | — | LISTO |
| B-IAM-06 | `iam/HashingServiceImplTest.java` | `HashingServiceImpl` (BCrypt) | `encode` ≠ texto plano · `matches` verdadero y falso | — | LISTO |
| B-GRP-01 | `groups/GroupDomainTest.java` | `Group`, `GroupCode`, `ImgUrl`, `GroupProfile` | `GroupCode.random` cumple el formato · el código inválido lanza excepción · `ImgUrl` null → "" · `getLeader` y `getMemberCount` · `updateInformation` ignora los vacíos · `addTask` en una membresía nueva | BUG-12 | LISTO-PARCIAL |
| B-GRP-02 | `groups/GroupCommandServiceImplTest.java` | `GroupCommandServiceImpl` | crear: el creador queda líder y se regenera el código si colisiona · líder inexistente → 404 · actualizar: solo el líder · borrar: solo el líder, grupo inexistente → 404 · quitar miembro: OK, no está en el grupo → 404, líder → excepción (409 esperado) | BUG-10 | LISTO-PARCIAL |
| B-GRP-03 | `groups/GroupQueryServiceImplTest.java` | `GroupQueryServiceImpl` | grupos por rol filtra por `GroupRole` · grupos por usuario · miembros de un grupo inexistente → 404 · por código con formato inválido → `IllegalArgumentException` | — | LISTO |
| B-GRP-04 | `groups/InvitationCommandServiceImplTest.java` | `InvitationCommandServiceImpl` | crear: OK solo el líder · invitación duplicada → 409 · ya es miembro → 409 · usuario o grupo inexistente → 404 · aceptar: solo el invitado; crea `GROUP_MEMBER` y borra la invitación; ya es miembro → 409 · cancelar: el invitado o el líder sí, un tercero → 403 | — | LISTO |
| B-GRP-05 | `groups/InvitationQueryServiceImplTest.java` | `InvitationQueryServiceImpl`, `GroupsContextFacadeImpl` | invitaciones por usuario y por grupo · `fetchLeaderIdByGroupId` (con líder, sin líder, sin grupo) | — | LISTO |
| B-TSK-01 | `tasks/TaskTest.java` | Agregado `Task` | nace `IN_PROGRESS` · `IN_PROGRESS`→`COMPLETED` calcula `timePassed` · `COMPLETED`/`ON_HOLD`/`EXPIRED` → `IN_PROGRESS` suma `timesRearranged` · `updateTask` con fecha pasada → `EXPIRED`, con fecha futura → `IN_PROGRESS` · ignora los campos vacíos · `applySubmissionReview` → `DONE`/`IN_PROGRESS` · rechaza un título vacío | BUG-03 | LISTO-PARCIAL |
| B-TSK-02 | `tasks/TaskCommandServiceImplTest.java` | `TaskCommandServiceImpl` | crear: solo el líder; el asignado debe ser miembro · actualizar: solo el líder, reasigna la membresía · borrar: solo el líder · estado: el líder o el asignado sí, un tercero → 403 · borrar las tareas de un miembro | — | LISTO |
| B-TSK-03 | `tasks/TaskStatusDoneBlockedTest.java` | `PUT /status` con `DONE` | `DONE` directo → 409 incluso para el líder | — | PREVIO |
| B-TSK-04 | `tasks/TaskQueryServiceImplTest.java` | `TaskQueryServiceImpl` | por grupo con y sin estado · por grupo y usuario · por id · por estado (texto) | — | LISTO |
| B-TSK-05 | `tasks/TaskStatusSchedulerTest.java` | `TaskStatusScheduler` | marca `EXPIRED` las tareas vencidas y crea una request `EXPIRED` sin solicitante · sin vencidas no hace nada · ejecuciones concurrentes no duplican | BUG-09 | LISTO-PARCIAL |
| B-TSK-06 | `tasks/TaskContextFacadeImplTest.java` | `TaskContextFacadeImpl` | `getTaskById` · `applySubmissionReview` guarda el nuevo estado · tarea inexistente → 404 · `deleteTasksByUserIdAndGroupId` delega | — | LISTO |
| B-REQ-01 | `requests/RequestCommandServiceImplTest.java` | `RequestCommandServiceImpl` | (18 casos previos: creación, revisión, permisos y efectos en la tarea) | — | PREVIO |
| B-REQ-02 | `requests/RequestQueryServiceImplTest.java` | `RequestQueryServiceImpl` | (5 casos previos: orden, filtros, líder, grupo inexistente, filtro inválido) | — | PREVIO |
| B-REQ-03 | `requests/RequestDomainAndDeleteTest.java` | `Request`, `RequestType`, `RequestStatus`, borrado | `fromString` sin distinguir mayúsculas e inválido · `Request` nace `PENDING` · cambio de estado · borrar una request inexistente → 400 · `DeleteAllRequestsByTaskId` (permisos del borrado: ver B-API-03, SEC-02) | — | LISTO |
| B-MET-01 | `metrics/TaskMetricsQueryServiceImplTest.java` | `TaskMetricsQueryServiceImpl` | resumen por estado · reagendados (total e ids) · distribución por integrante · tiempo promedio solo de `DONE` (en días) · grupo inexistente → 0 · duración solo `IN_PROGRESS` · `timePassed` solo `COMPLETED`/`DONE` · las consultas por usuario filtran por grupo | BUG-08 | LISTO-PARCIAL |
| B-AI-01 | `ai/TaskClassificationQueryServiceImplTest.java` | `TaskClassificationQueryServiceImpl` | (3 casos previos: mapeo, normalización, fallo de Bedrock) | — | PREVIO |
| B-AI-02 | `ai/TaskClassificationQueryServiceImplIT.java` | Bedrock real | (previo; se omite salvo con `RUN_BEDROCK_IT=1` y credenciales de AWS) | — | PREVIO |
| B-AI-03 | `ai/GroupReportQueryServiceImplTest.java` | `GroupReportQueryServiceImpl` | los hechos incluyen el resumen, los reagendados y una fila por integrante con la carga ponderada (difficulty, o 3 por defecto) · devuelve el texto del modelo · fallo de Bedrock → vacío | — | LISTO |
| B-SHR-01 | `shared/GlobalExceptionHandlerTest.java` | `GlobalExceptionHandler` | 400 / 401 / 403 / 404 / 409 / 500 con `ErrorResponse` y `path` · el 500 registra la excepción · timestamp con zona | BUG-07, BUG-14 | LISTO-PARCIAL |
| B-SHR-02 | `shared/NamingStrategyTest.java` | `SnakeCaseWithPluralizedTablePhysicalNamingStrategy` | tabla `GroupProfile` → `group_profiles` · columna `dueDate` → `due_date` · null → null | — | LISTO |

## Backend: pruebas de API (integración)

`@SpringBootTest(webEnvironment = RANDOM_PORT)` contra PostgreSQL real, usando HTTP con `java.net.http.HttpClient` (sin mocks). Cada prueba crea usuarios con un sufijo aleatorio. Ruta: `backend/src/test/java/com/hub/synhub_backend/api/`.

| ID | Archivo | Objetivo | Casos | Auditoría | Estado |
|---|---|---|---|---|---|
| B-API-00 | `api/ApiTestSupport.java` | Utilidad compartida | cliente HTTP, registro y login, creación de grupo, invitación y tarea | — | LISTO |
| B-API-01 | `api/AuthApiTests.java` | IAM por HTTP | sign-up 201 · sign-in 200 con token · contraseña incorrecta 401 · sin token 401 · token alterado 401 · `/users/me` · `/actuator/health` público · rol `ROLE_ADMIN` ignorado · email duplicado 409 · rol inválido no da 500 | SEC-05, BUG-04, BUG-05 | LISTO-PARCIAL |
| B-API-02 | `api/TaskLifecycleApiTests.java` | Flujo completo | crear grupo → invitar → aceptar → miembro listado · crear tarea → miembro `COMPLETED` → entrega → líder aprueba → `DONE` · rechazar → `IN_PROGRESS` · `DONE` directo 409 · segunda entrega pendiente 409 · bandeja del líder filtra por estado | — | LISTO |
| B-API-03 | `api/AuthorizationApiTests.java` | Matriz de permisos | (vigentes) miembro no puede editar ni borrar el grupo, ni invitar, ni crear o borrar tareas, ni revisar · ajeno no puede cambiar el estado · (esperados) ajeno → 403 en las lecturas del grupo, tareas, requests e invitaciones · ajeno no borra requests · `GET /users` solo admin | SEC-01, SEC-02, SEC-06 | LISTO-PARCIAL |
| B-API-04 | `api/ValidationApiTests.java` | Validación de entrada | (vigentes) `GroupCode` mal formado 400 · código inexistente 404 · tipo de request inválido 400 · un miembro no crea requests `EXPIRED` · (esperados) tarea sin título 400 · grupo sin nombre 400 · registro débil 400 · difficulty fuera de 1..5 400 · enum inválido en un parámetro 400 | SEC-07, BUG-03, BUG-06 | LISTO-PARCIAL |
| B-API-05 | `api/CascadeDeleteApiTests.java` | Borrados | (vigente) borrar un grupo vacío 204 · borrar una tarea sin requests 204 · (esperados) tarea con requests 204 · grupo con tareas y requests 204 | BUG-01, BUG-02 | LISTO-PARCIAL |

## Frontend

Vitest + Angular TestBed. Ruta base: `frontend/src/app/`.

| ID | Archivo | Objetivo | Casos | Auditoría | Estado |
|---|---|---|---|---|---|
| F-IAM-01 | `iam/application/iam.store.spec.ts` | `IamStore` | signIn guarda el token y el userId, carga el perfil y navega a `/home` · un error de signIn no inicia sesión · signUp navega al login · signOut limpia y navega · restoreSession sin token no llama a la API · restoreSession con un token inválido limpia la sesión | — | PENDIENTE |
| F-IAM-02 | `iam/infrastructure/iam.interceptor.spec.ts` | `iamInterceptor` | agrega Bearer · no lo agrega en sign-in ni sign-up · no agrega `undefined`/`null` · un 401 cierra la sesión y redirige | SEC-10 | PENDIENTE |
| F-IAM-03 | `iam/infrastructure/iam.guard.spec.ts` | `iamGuard` | con sesión permite · sin sesión redirige a `/auth/sign-in` · un token vencido redirige | SEC-10 | PENDIENTE |
| F-IAM-04 | `iam/infrastructure/iam.assemblers.spec.ts` | Assemblers de sign-in, sign-up, users y profiles | comando → request · respuesta → recurso o entidad · `Profile.userId` | BUG-15 | PENDIENTE |
| F-IAM-05 | `iam/infrastructure/iam.api.spec.ts` | `IamApi` (HTTP) | URLs y verbos: sign-in, sign-up, `/users/me`, `/users/{id}`, `/users/username?username=` codificado | — | PENDIENTE |
| F-IAM-06 | `iam/presentation/views/sign-in-page/sign-in-page.spec.ts` | `SignInPage` | (previo) se crea | — | PENDIENTE |
| F-IAM-07 | `iam/presentation/views/sign-up-page/sign-up-page.spec.ts` | `SignUpPage` | (previo) se crea · (esperado) contraseña < 8 caracteres inválida | FE-03 | PENDIENTE |
| F-SHR-01 | `shared/application/toast.store.spec.ts` | `ToastStore` | success, info y error agregan un toast · dismiss · los mensajes técnicos con códigos HTTP se vuelven amigables · desaparece por tiempo | — | PENDIENTE |
| F-SHR-02 | `shared/infrastructure/base-api-endpoint.spec.ts` | `BaseApiEndpoint` | getAll (arreglo y objeto envoltorio), getById, create, update y delete: URL, verbo y mapeo · `handleError` con 404 / estado / ErrorEvent | — | PENDIENTE |
| F-SHR-03 | `shared/presentation/components/base-form/base-form.spec.ts` | `BaseForm` | control inválido y tocado · mensajes `required` y genérico | — | PENDIENTE |
| F-SHR-04 | `shared/presentation/components/toast-container/toast-container.spec.ts` | `ToastContainer` | renderiza los toasts del store y los cierra | — | PENDIENTE |
| F-SHR-05 | `shared/presentation/*` previos | `Layout`, `Home`, `PageNotFound`, `App` | (previos) se crean | — | PENDIENTE |
| F-GRP-01 | `groups/application/groups.store.spec.ts` | `GroupsStore` | loadGroups separa líder y miembro y los une sin duplicar · error → toast + error · loadGroupMembers por grupo · addGroup agrega a líderes · updateGroup reemplaza · getGroupById | — | PENDIENTE |
| F-GRP-02 | `groups/infrastructure/groups.assembler.spec.ts` | `GroupsAssembler` | resource ↔ entidad · `memberCount` por defecto 0 · miembros con `Profile` · comando → request | — | PENDIENTE |
| F-GRP-03 | `groups/infrastructure/groups.api.spec.ts` | `GroupsApi` (HTTP) | `/groups/user/role?groupRole=` · POST `/groups` · PUT `/groups/{id}` · GET members | — | PENDIENTE |
| F-GRP-04 | `groups/presentation/**` previos | Vistas y componentes de groups | (7 previos) se crean | — | PENDIENTE |
| F-INV-01 | `invitations/application/invitations.store.spec.ts` | `InvitationsStore` | inviteByUsername busca el perfil y crea la invitación · username vacío no llama · usuario inexistente → error amable · accept quita de la lista y llama a onSuccess · decline quita de las dos listas · loadInvitations y loadGroupInvitations | — | PENDIENTE |
| F-INV-02 | `invitations/infrastructure/invitations.api.spec.ts` | `InvitationsApi` (HTTP) | URLs: POST, accept, DELETE, `/user`, `/group?groupId=` · mensajes en español para 403, 404 y 409 (ya miembro / pendiente) | — | PENDIENTE |
| F-INV-03 | `invitations/infrastructure/invitations.assembler.spec.ts` | `InvitationsAssembler` | resource → entidad (`user` → `profile`) · comando → request | — | PENDIENTE |
| F-TSK-01 | `tasks/application/tasks.store.spec.ts` | `TasksStore` | load por grupo, por usuario y multigrupo · `filteredTasks` por estado · addTask, updateTask, updateTaskStatus y deleteTask actualizan la lista · una tarea `DONE` no se edita ni se borra (error) · error → toast | — | PENDIENTE |
| F-TSK-02 | `tasks/infrastructure/tasks.api.spec.ts` | `TasksApi` (HTTP) | `status` como query param · `/tasks/group/{g}` con y sin estado · `/tasks/group/{g}/user/{u}` · PUT y DELETE | — | PENDIENTE |
| F-TSK-03 | `tasks/infrastructure/tasks.assembler.spec.ts` | `TasksAssembler` | (previo: campos de IA) | — | PENDIENTE |
| F-TSK-04 | `tasks/presentation/components/task-board/task-board.spec.ts` | `TaskBoard` | agrupa por columnas de estado · emite `statusChange` al cambiar el select | — | PENDIENTE |
| F-TSK-05 | `tasks/presentation/components/task-card/task-card.spec.ts` | `TaskCard` | se crea · el miembro con una tarea `COMPLETED` puede enviar la entrega · en revisión no permite otra | — | PENDIENTE |
| F-TSK-06 | `tasks/presentation/views/*/*.spec.ts` | `LeaderTasks`, `MemberTasks`, `TaskForm`, `TaskDetails`, `TaskList` | se crean · `TaskForm` calcula `aiAccepted` (sin IA → undefined, igual → true, editada → false) | — | PENDIENTE |
| F-REQ-01 | `requests/application/requests.store.spec.ts` | `RequestsStore` | loadGroupSubmissions pide `type=SUBMISSION` con el estado · approve y reject quitan de la bandeja · submitTask crea y llama a onSuccess · `isUnderReview` · error → toast | — | PENDIENTE |
| F-REQ-02 | `requests/infrastructure/requests.api.spec.ts` | `RequestsApi` (HTTP) | URLs anidadas `/tasks/{t}/requests…` y `/groups/{g}/requests?status&type` · mensajes en español para 403, 404 y 409 en crear, listar y revisar | — | PENDIENTE |
| F-REQ-03 | `requests/infrastructure/requests.assembler.spec.ts` | `RequestsAssembler` | resource → entidad con la tarea anidada · comando → request | — | PENDIENTE |
| F-REQ-04 | `requests/presentation/views/validations/validations.spec.ts` | `Validations` | se crea | — | PENDIENTE |
| F-AI-01 | `ai/application/ai.store.spec.ts` | `AiStore` | (6 previos) | — | PENDIENTE |
| F-AI-02 | `ai/domain/model/task-classification.entity.spec.ts` | `parseReport` | (2 previos) | — | PENDIENTE |
| F-AI-03 | `ai/infrastructure/ai.api.spec.ts` | `AiApi` (HTTP) | POST `/ai/classify` con `dueDate` en ISO · GET report devuelve el texto | — | PENDIENTE |
| F-AI-04 | `ai/presentation/components/ai-report-card/ai-report-card.spec.ts` | `AiReportCard` | se crea · generar llama a `loadReport(groupId)` · sin grupo no llama | — | PENDIENTE |
| F-TOUR-01 | `tour/domain/model/tour-chapter.spec.ts` | Capítulos del tour | ids únicos · `start` creciente y menor que `TOUR_DURATION` · el primero empieza en 0 | — | PENDIENTE |
| F-TOUR-02 | `tour/presentation/views/tour-page/tour-page.spec.ts` | `TourPage` | se crea · `currentIndex` según `currentTime` · `progress` de los segmentos | — | PENDIENTE |
| F-APP-01 | `app.routes.spec.ts` | Rutas | las rutas privadas tienen `iamGuard` · `''` redirige al login · `**` → `PageNotFound` | — | PENDIENTE |
| F-ENV-01 | `environments.spec.ts` | Entornos | producción usa `/api/v1` relativo · desarrollo usa localhost · los dos definen los mismos paths | — | PENDIENTE |

## Fuera de alcance (pendiente de decidir)

| Tipo | Motivo |
|---|---|
| E2E en navegador (Playwright/Cypress) | No hay infraestructura; los flujos principales se cubren con B-API-02 a nivel HTTP. |
| Carga y rendimiento | Depende de PERF-01 (N+1). |
| Contrato frontend ↔ backend (por ejemplo, OpenAPI → tipos) | Mejora futura; hoy los assemblers y specs de API cubren el mapeo. |

## Bitácora

| Fecha | Avance | Siguiente |
|---|---|---|
| 2026-10-13 | Se crea el inventario (69 archivos, todos `PENDIENTE`). | B-IAM-01 |
