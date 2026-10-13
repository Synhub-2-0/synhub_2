# Requests: backend

Paquete: [`com.hub.synhub_backend.requests`](../../../../backend/src/main/java/com/hub/synhub_backend/requests)

## Endpoints

Todos requieren token.

### `RequestController`: `/api/v1/tasks/{taskId}/requests`

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/` | `CreateRequestResource { description, requestType }` | `201 RequestResource` | Ver "Reglas al crear". |
| `GET` | `/` | — | `200 RequestResource[]` · `403` · `404` | Solo integrantes del grupo de la tarea. |
| `GET` | `/{requestId}` | — | `200` · `404` · `400` si la request es de otra tarea | Solo integrantes del grupo de la tarea (403). |
| `PUT` | `/{requestId}/status/{status}` | `status` = `APPROVED` \| `REJECTED` | `200 RequestResource` | Ver "Reglas al revisar". |
| `DELETE` | `/{requestId}` | — | `204` · `403` · `404` (no existe o es de otra tarea) | El líder del grupo borra cualquiera; el integrante asignado, solo las suyas en `PENDING` que no sean `EXPIRED`. |

### `GroupRequestController`: `/api/v1/groups/{groupId}/requests`

| Método | Parámetros | Respuesta | Regla |
|---|---|---|---|
| `GET` | `?status=` y `?type=` (opcionales; no distinguen mayúsculas) | `200 RequestResource[]`, de la más nueva a la más antigua | Solo el líder del grupo (403). 404 si el grupo no existe. 400 si el filtro tiene un valor desconocido. |

`RequestResource = { id, description, requestType, requestStatus, task: TaskResource }`

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Request` (tabla `requests`) | `description`, `requestType`, `requestStatus` (se crea `PENDING`) y `task` (`@ManyToOne` hacia el `Task` de tasks). Los getters de tipo y estado devuelven `String`. |
| Value object | `RequestType` | `SUBMISSION`, `MODIFICATION`, `EXPIRED`. `fromString` no distingue mayúsculas. |
| Value object | `RequestStatus` | `PENDING`, `APPROVED`, `REJECTED`. |

## Reglas (`RequestCommandServiceImpl`)

**Al crear** (`CreateRequestCommand(description, requestType, taskId, requesterId)`):

1. Si el tipo no es válido → 400. Si la tarea no existe (`TasksContextFacade.getTaskById`) → 404.
2. Si `requesterId != null` (lo crea un usuario):
   - `EXPIRED` → 400: solo lo genera el sistema.
   - Solo el integrante asignado a la tarea → si no, 403.
   - `SUBMISSION`: la tarea debe estar `COMPLETED` (409 `TaskNotCompletedException`) y no puede existir otra `SUBMISSION` en `PENDING` (409 `PendingSubmissionAlreadyExistsException`).
3. Si `requesterId == null` (lo crea el sistema, desde `TaskStatusScheduler`), se omiten las validaciones de usuario.

**Al revisar** (`UpdateRequestCommand`):

1. La request debe pertenecer a la tarea de la URL (404).
2. Solo el líder del grupo de la tarea (403).
3. Nuevo estado `PENDING` → 400. Si la request ya fue revisada → 409 `RequestAlreadyReviewedException`.
4. Si es `SUBMISSION`:
   - `APPROVED`: exige la tarea en `COMPLETED` (409 si no lo está) y llama a `applySubmissionReview(taskId, true)` → `DONE`.
   - `REJECTED`: si la tarea sigue `COMPLETED`, llama a `applySubmissionReview(taskId, false)` → `IN_PROGRESS`; si no, solo cierra la request.
5. `MODIFICATION` y `EXPIRED` no modifican la tarea.

`RequestQueryServiceImpl` maneja `GetRequestsByTaskIdQuery` y `GetRequestByIdQuery` (solo integrantes del grupo de la tarea, vía `TasksContextFacade` + `GroupsContextFacade.isMember`), `GetRequestsByGroupIdQuery` (valida al líder con `GroupsContextFacade` y filtra en memoria) y `GetAllRequestsQuery` (sin uso).

## Dependencias

| Hacia | Mediante | Para |
|---|---|---|
| tasks | `TasksContextFacade` (ACL) | Cargar la tarea y aplicar el resultado de la revisión. |
| groups | `GroupsContextFacade` (ACL) | Saber quién es el líder del grupo y si el solicitante es integrante. |

Requests también **ofrece** una ACL: `RequestsContextFacade.deleteAllRequestsByTaskId`, que usa tasks antes de borrar una tarea. Su implementación usa `RequestRepository` directamente para no formar un ciclo de beans con `RequestCommandService` → `TasksContextFacade`.
| iam | `UserQueryService` (en los controladores) | Resolver el usuario del token. |

## Persistencia

`Request` usa borrado lógico (`deleted_at`, HUM-03).

`RequestRepository`: `findByTaskId`, `deleteByTaskId`, `findByTask_Profile_Group_Id` y `existsByTask_IdAndRequestTypeAndRequestStatus`.

## Errores

| Excepción | HTTP |
|---|---|
| `IllegalArgumentException` | 400 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `TaskNotCompletedException`, `PendingSubmissionAlreadyExistsException`, `RequestAlreadyReviewedException` | 409 |

## Pruebas

- `RequestCommandServiceImplTest` (18 casos): creación, revisión, permisos y efectos sobre la tarea.
- `RequestQueryServiceImplTest` (5 casos): orden, filtros, permiso de líder y grupo inexistente.

## Observaciones

