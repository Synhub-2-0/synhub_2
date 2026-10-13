# Tasks: backend

Paquete: [`com.hub.synhub_backend.tasks`](../../../../backend/src/main/java/com/hub/synhub_backend/tasks). La capa de aplicación está en `aplication/` (con una sola "p").

## Endpoints (`TaskController`)

Todos requieren token.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/tasks` | `CreateTaskResource` | `200 TaskResource` | Solo el líder del grupo; el asignado debe ser integrante (404 si no). |
| `GET` | `/api/v1/tasks/{taskId}` | — | `200 TaskResource` · `403` · `404` | Solo integrantes del grupo de la tarea. |
| `GET` | `/api/v1/tasks/group/{groupId}` | `?status=` (opcional, `TaskStatus`) | `200 TaskResource[]` · `403` | Solo integrantes del grupo. |
| `GET` | `/api/v1/tasks/group/{groupId}/user/{userId}` | — | `200 TaskResource[]` · `403` | Solo integrantes del grupo. |
| `PUT` | `/api/v1/tasks/{taskId}` | `UpdateTaskResource` | `200 TaskResource` | Solo el líder. Puede reasignar a otro integrante. |
| `PUT` | `/api/v1/tasks/{taskId}/status` | `?status=` | `200 TaskResource` | Líder o asignado. `DONE` → 409. |
| `DELETE` | `/api/v1/tasks/{taskId}` | — | `204` | Solo el líder. |

```text
CreateTaskResource = { title, description, dueDate (OffsetDateTime), userId, groupId,
                       difficulty?, context?, urgency?, labels?, aiAccepted? }
UpdateTaskResource = { requesterId, title, description, dueDate, userId }
TaskResource       = { id, title, description, dueDate, createdAt, updatedAt, status,
                       group: GroupResourceReduced, assignedTo: PublicUserResource,
                       difficulty, context, urgency, labels, aiAccepted }
```

`UpdateTaskResource.requesterId` se ignora: el solicitante siempre se toma del token.

**Depuración**: `TestController` (`/api/v1/tests/fetch-user` y `/api/v1/tests/token`) se activa solo cuando el perfil activo no es `prod` (`@Profile("!prod")`).

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Task` | `title` (obligatorio), `description` (vacía si no se indica), `status`, `dueDate` (obligatoria), `profile: GroupProfile` (groups), campos de IA (`difficulty`, `context`, `urgency`, `labels` como texto separado por comas, `aiAccepted`), y métricas (`timesRearranged`, `timePassed` en ms). |
| Value object | `TaskStatus` | `ON_HOLD`, `IN_PROGRESS`, `COMPLETED`, `DONE`, `EXPIRED`. |

Comportamiento del agregado:

- `updateStatus(cmd)`: al pasar de `IN_PROGRESS` a `COMPLETED` acumula `timePassed` (desde `createdAt`, o desde `updatedAt` si ya fue reagendada). Las transiciones `COMPLETED`, `ON_HOLD` o `EXPIRED` → `IN_PROGRESS` incrementan `timesRearranged`.
- `updateTask(cmd)`: actualiza los campos no vacíos; si cambia `dueDate`, recalcula `EXPIRED` / `IN_PROGRESS`.
- `applySubmissionReview(approved)`: `DONE` si se aprueba, `IN_PROGRESS` si se rechaza. Solo lo invoca requests.

## Capa de aplicación

| Servicio | Mensajes |
|---|---|
| `TaskCommandServiceImpl` | `CreateTaskCommand`, `UpdateTaskCommand` (incluye reasignación entre membresías), `DeleteTaskCommand`, `UpdateTaskStatusCommand` y `DeleteTasksByUserIdAndGroupIdCommand`. |
| `TaskQueryServiceImpl` | `GetTaskByIdQuery`, `GetAllTasksQuery`, `GetAllTasksByUserId`, `GetAllTaskByStatusQuery`, `GetAllTasksByGroupIdQuery`, `GetTasksByGroupIdAndStatus`, `GetTasksByGroupIdAndUserIdQuery`. |
| `TaskStatusScheduler` (`domain/services`) | `@Scheduled(fixedRate = 30000)`: busca las tareas `IN_PROGRESS` vencidas, las pasa a `EXPIRED` y crea `CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", taskId)` sin `requesterId`, por lo que se salta las validaciones de usuario. |

## ACL

Hacia requests: `RequestsContextFacade.deleteAllRequestsByTaskId`, que `DeleteTaskCommand` y `DeleteTasksByUserIdAndGroupIdCommand` llaman antes de borrar cada tarea (las requests la referencian por FK).

Hacia otros contextos:

`TasksContextFacade` (`interfaces/acl`) / `TaskContextFacadeImpl` (`aplication/internal/acl`):

| Método | Lo usa |
|---|---|
| `getTaskById` | requests (crear y eliminar solicitudes) |
| `applySubmissionReview` | requests (revisar entregas) |
| `deleteTasksByUserIdAndGroupId` | groups, mediante `ExternalTasksService` (sin llamadas por ahora) |
| `deleteAllTasksByGroupId` | groups, al borrar un grupo (`DeleteTasksByGroupIdCommand`: requests y luego tareas) |

## Persistencia

`TaskRepository`: `findByProfile_Id`, `findByStatus`, `findAllByStatusAndDueDateBefore`, `findByProfile_User_Id`, `findByProfile_Group_Id`, `findByProfile_Group_IdAndProfile_User_Id` y `findByProfile_Group_IdAndStatus`. Las consultas por grupo y por usuario **navegan la membresía de groups** (`profile.group`, `profile.user`).

## Errores

| Excepción | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `AccessDeniedException` | 403 |
| `TaskStatusTransitionNotAllowedException` | 409 |

## Pruebas

`TaskStatusDoneBlockedTest`: verifica que `PUT /status` no permita `DONE`.

## Observaciones

- El comentario de `TaskStatusScheduler` dice "cada 60 segundos", pero `fixedRate = 30000` son 30 s.
- `TaskStatusScheduler` es un proceso de aplicación ubicado en `domain/services`.
