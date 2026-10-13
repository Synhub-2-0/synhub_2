# Tasks: backend

Paquete: [`com.hub.synhub_backend.tasks`](../../../../backend/src/main/java/com/hub/synhub_backend/tasks). La capa de aplicación está en `aplication/` (con una sola "p").

## Endpoints (`TaskController`)

Todos requieren token.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/tasks` | `CreateTaskResource` | `200 TaskResource` | Solo el líder del grupo; el asignado debe ser integrante (404 si no). |
| `GET` | `/api/v1/tasks/user` | — | `200 TaskResource[]` por fecha límite | Las tareas asignadas al usuario de la sesión en todos sus grupos (`GetAllTasksByUserId`, AUDIT PERF-03). |
| `GET` | `/api/v1/tasks/{taskId}` | — | `200 TaskResource` · `403` · `404` | Solo integrantes del grupo de la tarea. |
| `GET` | `/api/v1/tasks/group/{groupId}` | `?status=` (opcional, `TaskStatus`); `?page=&size=` (opcionales) | `200 TaskResource[]`, o `PageResource<TaskResource>` ordenado por fecha límite si se pide página · `403` | Solo integrantes del grupo. |
| `GET` | `/api/v1/tasks/group/{groupId}/user/{userId}` | — | `200 TaskResource[]` · `403` | Solo integrantes del grupo. |
| `PUT` | `/api/v1/tasks/{taskId}` | `UpdateTaskResource` | `200 TaskResource` | Solo el líder. Puede reasignar a otro integrante. |
| `PUT` | `/api/v1/tasks/{taskId}/status` | `?status=` | `200 TaskResource` | Líder o asignado. `DONE` → 409. Una tarea que ya está `DONE` no cambia de estado (409). |
| `DELETE` | `/api/v1/tasks/{taskId}` | — | `204` | Solo el líder. |

```text
CreateTaskResource = { title, description, dueDate (OffsetDateTime), userId, groupId,
                       difficulty?, context?, urgency?, labels?, aiAccepted? }
UpdateTaskResource = { title, description, dueDate, userId }
TaskResource       = { id, title, description, dueDate, createdAt, updatedAt, status,
                       group: GroupResourceReduced, assignedTo: PublicUserResource,
                       difficulty, context, urgency, labels, aiAccepted }
```

Quien edita se toma siempre de la sesión; el cuerpo no lo incluye (BUG-16).


## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Task` | `title` (obligatorio), `description` (vacía si no se indica), `status`, `dueDate` (obligatoria), `profile: GroupProfile` (groups), campos de IA (`difficulty`, `context`, `urgency`, `labels` como texto separado por comas, `aiAccepted`), y métricas (`timesRearranged`, `timePassed` en ms). |
| Value object | `TaskStatus` | `ON_HOLD`, `IN_PROGRESS`, `COMPLETED`, `DONE`, `EXPIRED`. |

Comportamiento del agregado:

- `updateStatus(cmd)`: al pasar de `IN_PROGRESS` a `COMPLETED` acumula `timePassed` (desde `createdAt`, o desde `updatedAt` si ya fue reagendada). Las transiciones `COMPLETED`, `ON_HOLD` o `EXPIRED` → `IN_PROGRESS` incrementan `timesRearranged`.
- `updateTask(cmd)`: actualiza los campos no vacíos; si cambia `dueDate`, recalcula `EXPIRED` / `IN_PROGRESS`.
- `applySubmissionReview(approved)`: `DONE` si se aprueba, `IN_PROGRESS` si se rechaza. Solo lo invoca requests.
- `ensureNotFinished()`: una tarea `DONE` es definitiva; editarla, cambiarle el estado o borrarla responde 409 (`TaskFinishedException`, AUDIT BUG-18).

## Capa de aplicación

| Servicio | Mensajes |
|---|---|
| `TaskCommandServiceImpl` | `CreateTaskCommand`, `UpdateTaskCommand` (incluye reasignación entre membresías), `DeleteTaskCommand`, `UpdateTaskStatusCommand` y `DeleteTasksByUserIdAndGroupIdCommand`. |
| `TaskQueryServiceImpl` | `GetTaskByIdQuery`, `GetAllTasksByUserId`, `GetAllTasksByGroupIdQuery`, `GetTasksByGroupIdAndStatus`, `GetTasksByGroupIdAndUserIdQuery`. |
| `TaskStatusScheduler` (`aplication/internal/schedulers`) | `@Scheduled` cada `app.tasks.expiration-check-ms` (60 s; variable `TASKS_EXPIRATION_CHECK_MS`): busca las tareas `IN_PROGRESS` vencidas y, en una transacción por tarea, las marca con un `UPDATE` condicional (`TaskRepository.markExpiredIfInProgress`, `WHERE status = IN_PROGRESS`). Solo si cambió la fila crea `CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", taskId)` sin `requesterId`, por lo que se salta las validaciones de usuario. Con varias instancias, solo una crea la request (AUDIT BUG-09). |

## ACL

Hacia requests: `RequestsContextFacade.deleteAllRequestsByTaskId`, que `DeleteTaskCommand` y `DeleteTasksByUserIdAndGroupIdCommand` llaman antes de borrar cada tarea. El borrado es lógico (HUM-03): `Task` y `Request` se marcan con `deleted_at`, dejan de ser visibles y la tarea conserva su perfil (responsable y grupo).

Hacia otros contextos:

`TasksContextFacade` (`interfaces/acl`) / `TaskContextFacadeImpl` (`aplication/internal/acl`):

| Método | Lo usa |
|---|---|
| `getTaskById` | requests (crear y eliminar solicitudes) |
| `applySubmissionReview` | requests (revisar entregas) |
| `deleteTasksByUserIdAndGroupId` | groups, al quitar a un integrante o cuando sale del grupo |
| `deleteAllTasksByGroupId` | groups, al borrar un grupo (`DeleteTasksByGroupIdCommand`: requests y luego tareas) |

## Persistencia

`TaskRepository`: `findByProfile_Id`, `findAllByStatusAndDueDateBefore`, `markExpiredIfInProgress`, `findByProfile_User_Id`, `findByProfile_Group_Id`, `findByProfile_Group_IdAndProfile_User_Id` y `findByProfile_Group_IdAndStatus`. Las consultas por grupo y por usuario **navegan la membresía de groups** (`profile.group`, `profile.user`).

## Errores

| Excepción | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `AccessDeniedException` | 403 |
| `TaskStatusTransitionNotAllowedException` | 409 |

## Pruebas

`TaskStatusDoneBlockedTest`: verifica que `PUT /status` no permita `DONE`.

## Observaciones

