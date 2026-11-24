# API REST: convenciones y catálogo

## Convenciones

| Aspecto | Convención |
|---|---|
| Base | `/api/v1` |
| Formato | JSON en ambos sentidos; fechas en ISO-8601 (`OffsetDateTime`) |
| Autenticación | Cookie `HttpOnly` `SYNHUB_SESSION` (la web) o `Authorization: Bearer <jwt>` (clientes de API). La sesión dura 1 día. Son públicas solo `/api/v1/authentication/**`, `/actuator/health` y Swagger (perfil `dev`). |
| Identidad | El usuario que actúa **siempre** se toma del token; nunca de un campo del cuerpo. |
| Recursos | Sustantivo en plural por recurso raíz (`/groups`, `/tasks`, `/invitations`); relaciones como sub-recursos (`/groups/{groupId}/members`); las solicitudes (`/requests`) son un recurso raíz y reciben la tarea o el grupo como `taskId` / `groupId`. |
| Vistas del usuario actual | Segmento `user` o `me` (`/groups/user`, `/invitations/user`, `/users/me`). |
| Filtros | Query params opcionales (`?status=`, `?type=`, `?groupRole=`). |
| Validación | Los cuerpos se validan con Bean Validation (`@Valid`): un campo obligatorio ausente, un formato inválido o un valor fuera de rango responden `400` con `message` = `Invalid request: campo: motivo; …`. |
| Verbos | `GET` lee, `POST` crea (o ejecuta una acción: `/invitations/accept`), `PUT` actualiza y `DELETE` borra. |
| Códigos | `200`/`201` éxito, `204` borrado, `400` datos inválidos, `401` sin token, `403` sin permiso de negocio, `404` no existe, `409` regla de negocio, `503` servicio externo (Bedrock) no disponible. |
| Errores | `{ timestamp, status, error, message, path }` (`ErrorResponse`). |
| Paginación | Opcional en `GET /tasks/group/{id}` y `GET /requests/group?groupId=`: con `?page` (desde 0) o `?size` (por defecto 20, máximo 100) responden `PageResource { content, page, size, totalElements, totalPages }`; sin ellos, el arreglo completo. `page` negativo o `size` < 1 → 400. |
| Documentación viva | Swagger UI en `/swagger-ui.html`; OpenAPI en `/v3/api-docs`. Solo con el perfil `dev`. |

**Desviaciones**: `POST /groups`, `POST /tasks` y `POST /invitations` responden `200` en lugar de `201`. La revisión de una request usa un segmento de ruta (`PUT …/status/{status}`), mientras que en tasks el estado va como query param (`PUT /tasks/{id}/status?status=`).

## Catálogo

Columna **Consumidor**: método del frontend que hace la petición ("—" = la web no lo usa).

### IAM: [docs](../features/iam/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/authentication/sign-in` | `AuthenticationController` | `IamStore.signIn` |
| POST | `/authentication/sign-up` | `AuthenticationController` | `IamStore.signUp` |
| GET | `/users` | `UsersController` | — |
| GET | `/users/me` | `UsersController` | `IamStore.restoreSession` |
| GET | `/users/{userId}` | `UsersController` | `IamStore.signIn`, `IamStore.restoreSession` (perfil) |
| GET | `/users/username?username=` | `UsersController` | `InvitationsStore.inviteByUsername` |
| GET | `/roles` | `RolesController` | — |

### Groups: [docs](../features/groups/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/groups` | `GroupController` | `GroupsStore.addGroup` |
| GET | `/groups/{groupId}` | `GroupController` | — |
| GET | `/groups/search?code=` | `GroupController` | — |
| POST | `/groups/join` | `GroupController` | `GroupsStore.joinGroupByCode` |
| DELETE | `/groups/{groupId}/members/{userId}` | `GroupController` | `GroupsStore.removeMember`, `GroupsStore.leaveGroup` |
| GET | `/groups/{groupId}/members` | `GroupController` | `GroupsStore.loadGroupMembers` |
| GET | `/groups/user` | `GroupController` | — |
| GET | `/groups/user/role?groupRole=` | `GroupController` | `GroupsStore.loadGroups` |
| PUT | `/groups/{groupId}` | `GroupController` | `GroupsStore.updateGroup` |
| DELETE | `/groups/{groupId}` | `GroupController` | `GroupsStore.deleteGroup` |
| POST | `/groups/{groupId}/activate` (solo el líder; 409 sin cupo o si ya está activo) | `GroupController` | `GroupsStore.activateGroup` |

### Invitations: [docs](../features/invitations/README.md) (código backend en `groups`)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/invitations` | `InvitationController` | `InvitationsStore.inviteByUsername` |
| POST | `/invitations/accept?invitationId=` | `InvitationController` | `InvitationsStore.acceptInvitation` |
| DELETE | `/invitations/{invitationId}` | `InvitationController` | `InvitationsStore.declineInvitation` |
| GET | `/invitations/user` | `InvitationController` | `InvitationsStore.loadInvitations` |
| GET | `/invitations/group?groupId=` | `InvitationController` | `InvitationsStore.loadGroupInvitations` |

### Tasks: [docs](../features/tasks/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/tasks` | `TaskController` | `TasksStore.addTask` |
| GET | `/tasks/{taskId}` | `TaskController` | `TasksStore.loadTaskById` |
| GET | `/tasks/group/{groupId}?status=&page=&size=` | `TaskController` | `TasksStore.loadTasksByGroup` |
| GET | `/tasks/user` | `TaskController` | `TasksStore.loadMyTasks` |
| GET | `/tasks/group/{groupId}/user/{userId}` | `TaskController` | `TasksStore.loadTasksByGroupAndUser` |
| PUT | `/tasks/{taskId}` (resuelve la `MODIFICATION` pendiente) | `TaskController` | `TasksStore.updateTask`, `resumeExpiredTask` |
| PUT | `/tasks/{taskId}/status?status=` (409 a `IN_PROGRESS` con una modificación pendiente) | `TaskController` | `TasksStore.updateTaskStatus`, `resumeExpiredTask` |
| DELETE | `/tasks/{taskId}` | `TaskController` | `TasksStore.deleteTask` |

### Subscriptions: [docs](../features/subscriptions/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| GET | `/subscriptions/me` | `SubscriptionController` | `SubscriptionsStore.load` |
| PUT | `/subscriptions/me` (cuerpo: `plan`, `activeGroupIds?`; pago simulado) | `SubscriptionController` | `SubscriptionsStore.changePlan` |

**Grupo bloqueado**: en un grupo bloqueado (su líder superó el límite de su plan) toda escritura de groups, invitations, tasks y requests, y `GET /ai/groups/{id}/report`, responde **409** (`GroupLockedException`). Las lecturas, salir del grupo, borrarlo y cancelar invitaciones siguen permitidos.

### Requests: [docs](../features/requests/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/requests` (cuerpo: `taskId`, `description`, `requestType`) | `RequestController` | `RequestsStore.submitTask`, `createModification` |
| GET | `/requests/task?taskId=` | `RequestController` | `RequestsStore.loadTaskRequests` |
| GET | `/requests/{requestId}` | `RequestController` | — |
| PUT | `/requests/{requestId}/status/{status}` (`MODIFICATION`: `REJECTED` la deniega y reanuda la tarea; `APPROVED` responde 409, se aprueba editando la tarea) | `RequestController` | `RequestsStore.approveRequest`, `rejectRequest` |
| DELETE | `/requests/{requestId}` (409 con una `MODIFICATION` pendiente) | `RequestController` | — |
| GET | `/requests/group?groupId=&status=&type=&page=&size=` | `RequestController` | `RequestsStore.loadGroupRequests` |

### AI: [docs](../features/ai/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/ai/classify` | `AiController` | `AiStore.classifyTask` |
| GET | `/ai/groups/{groupId}/report` | `AiController` | `AiStore.loadReport` |

### Metrics: [docs](../features/metrics/README.md)

| Método | Ruta | Backend | Frontend |
|---|---|---|---|
| GET | `/metrics/groups/{groupId}/{overview\|distribution\|rescheduled\|avg-completion-time}` | `MetricsController` | `MetricsApiEndpoint` (distribution y rescheduled en detalles del grupo) |
| GET | `/metrics/groups/{groupId}/members/{userId}/{overview\|distribution\|rescheduled\|avg-completion-time\|time-passed}` | `MetricsController` | — |
| GET | `/metrics/tasks/{taskId}/{time-passed\|duration}` | `MetricsController` | — |

### Infraestructura

| Método | Ruta | Uso |
|---|---|---|
| GET | `/actuator/health` | Health check del ALB y de Docker. |
| GET | `/swagger-ui.html`, `/v3/api-docs` | Documentación OpenAPI (solo perfil `dev`). |
