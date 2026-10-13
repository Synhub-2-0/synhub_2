# API REST: convenciones y catálogo

## Convenciones

| Aspecto | Convención |
|---|---|
| Base | `/api/v1` |
| Formato | JSON en ambos sentidos; fechas en ISO-8601 (`OffsetDateTime`) |
| Autenticación | Cookie `HttpOnly` `SYNHUB_SESSION` (la web) o `Authorization: Bearer <jwt>` (clientes de API). La sesión dura 1 día. Son públicas solo `/api/v1/authentication/**`, `/actuator/health` y Swagger (perfil `dev`). |
| Identidad | El usuario que actúa **siempre** se toma del token; nunca de un campo del cuerpo. |
| Recursos | Sustantivo en plural por recurso raíz (`/groups`, `/tasks`, `/invitations`); relaciones como sub-recursos (`/tasks/{taskId}/requests`, `/groups/{groupId}/members`). |
| Vistas del usuario actual | Segmento `user` o `me` (`/groups/user`, `/invitations/user`, `/users/me`). |
| Filtros | Query params opcionales (`?status=`, `?type=`, `?groupRole=`). |
| Validación | Los cuerpos se validan con Bean Validation (`@Valid`): un campo obligatorio ausente, un formato inválido o un valor fuera de rango responden `400` con `message` = `Invalid request: campo: motivo; …`. |
| Verbos | `GET` lee, `POST` crea (o ejecuta una acción: `/invitations/accept`), `PUT` actualiza y `DELETE` borra. |
| Códigos | `200`/`201` éxito, `204` borrado, `400` datos inválidos, `401` sin token, `403` sin permiso de negocio, `404` no existe, `409` regla de negocio, `503` servicio externo (Bedrock) no disponible. |
| Errores | `{ timestamp, status, error, message, path }` (`ErrorResponse`). |
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
| DELETE | `/groups/{groupId}` | `GroupController` | — |
| GET | `/groups/{groupId}/requests?status=&type=` | `GroupRequestController` (**requests**) | `RequestsStore.loadGroupSubmissions` |

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
| GET | `/tasks/group/{groupId}?status=` | `TaskController` | `TasksStore.loadTasksByGroup` |
| GET | `/tasks/group/{groupId}/user/{userId}` | `TaskController` | `TasksStore.loadTasksByGroupAndUser`, `loadTasksByGroupsAndUser`, `loadAssignedGroupIds` |
| PUT | `/tasks/{taskId}` | `TaskController` | `TasksStore.updateTask` |
| PUT | `/tasks/{taskId}/status?status=` | `TaskController` | `TasksStore.updateTaskStatus` |
| DELETE | `/tasks/{taskId}` | `TaskController` | `TasksStore.deleteTask` |

### Requests: [docs](../features/requests/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/tasks/{taskId}/requests` | `RequestController` | `RequestsStore.submitTask` |
| GET | `/tasks/{taskId}/requests` | `RequestController` | `RequestsStore.loadTaskRequests` |
| GET | `/tasks/{taskId}/requests/{requestId}` | `RequestController` | — |
| PUT | `/tasks/{taskId}/requests/{requestId}/status/{status}` | `RequestController` | `RequestsStore.approveSubmission`, `rejectSubmission` |
| DELETE | `/tasks/{taskId}/requests/{requestId}` | `RequestController` | — |
| GET | `/groups/{groupId}/requests` | `GroupRequestController` | ver Groups |

### AI: [docs](../features/ai/README.md)

| Método | Ruta | Controlador | Consumidor (frontend) |
|---|---|---|---|
| POST | `/ai/classify` | `AiController` | `AiStore.classifyTask` |
| GET | `/ai/groups/{groupId}/report` | `AiController` | `AiStore.loadReport` |

### Metrics: [docs](../features/metrics/README.md)

`/metrics`: sin endpoints.

### Infraestructura

| Método | Ruta | Uso |
|---|---|---|
| GET | `/actuator/health` | Health check del ALB y de Docker. |
| GET | `/swagger-ui.html`, `/v3/api-docs` | Documentación OpenAPI (solo perfil `dev`). |
