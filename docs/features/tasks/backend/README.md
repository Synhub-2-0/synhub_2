# Tasks: backend

Paquete: [`com.hub.synhub_backend.tasks`](../../../../backend/src/main/java/com/hub/synhub_backend/tasks).

## Endpoints (`TaskController`)

Todos requieren token.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/tasks` | `CreateTaskResource` | `200 TaskResource` | Solo el líder del grupo; el asignado debe ser integrante (404 si no). |
| `GET` | `/api/v1/tasks/user` | — | `200 TaskResource[]` por fecha límite | Las tareas asignadas al usuario de la sesión en todos sus grupos (`GetAllTasksByUserId`, AUDIT PERF-03). |
| `GET` | `/api/v1/tasks/{taskId}` | — | `200 TaskResource` · `403` · `404` | Solo integrantes del grupo de la tarea. |
| `GET` | `/api/v1/tasks/group/{groupId}` | `?status=` (opcional, `TaskStatus`); `?page=&size=` (opcionales) | `200 TaskResource[]`, o `PageResource<TaskResource>` ordenado por fecha límite si se pide página · `403` | Solo integrantes del grupo. |
| `GET` | `/api/v1/tasks/group/{groupId}/user/{userId}` | — | `200 TaskResource[]` · `403` | Solo integrantes del grupo. |
| `PUT` | `/api/v1/tasks/{taskId}` | `UpdateTaskResource` | `200 TaskResource` | Solo el líder. Puede reasignar a otro integrante y cambiar o quitar la clasificación. |
| `PUT` | `/api/v1/tasks/{taskId}/status` | `?status=` | `200 TaskResource` | **Solo el líder** (403 para el integrante asignado). `DONE` → 409. Una tarea que ya está `DONE` no cambia de estado (409). El integrante no cambia estados: al enviar su entrega la tarea pasa a `COMPLETED` por `POST /requests`. |
| `DELETE` | `/api/v1/tasks/{taskId}` | — | `204` | Solo el líder. |

```text
CreateTaskResource = { title, description, dueDate (OffsetDateTime), userId, groupId,
                       difficulty?, context?, urgency?, labels?, aiAccepted?, aiRationale? }
UpdateTaskResource = { title, description, dueDate, userId,
                       classification?: { difficulty, context, urgency, labels, aiAccepted, aiRationale } }
TaskResource       = { id, title, description, dueDate, createdAt, updatedAt, status,
                       group: GroupResourceReduced, assignedTo: PublicUserResource,
                       difficulty, context, urgency, labels, aiAccepted, aiRationale }
```

Quien edita se toma siempre de la sesión; el cuerpo no lo incluye (BUG-16).

En `PUT /tasks/{taskId}`, si `classification` no viene, la clasificación de la tarea no cambia. Si viene, reemplaza a la guardada: un campo nulo o vacío se quita, y si no queda ninguno también se borra `aiAccepted`.

`aiRationale` (máx. 500, columna `ai_rationale`, migración `V2__task_ai_rationale.sql`) es la justificación que dio la IA al sugerir la clasificación. Solo se guarda cuando la clasificación viene de la IA (`aiAccepted` no nulo): una clasificación hecha a mano o quitada la deja en nulo.


## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Task` | `title` (obligatorio), `description` (vacía si no se indica), `status`, `dueDate` (obligatoria), `profile: GroupProfile` (groups), campos de IA (`difficulty`, `context`, `urgency`, `labels` como texto separado por comas, `aiAccepted`, `aiRationale`), y métricas (`timesRearranged`, `timePassed` en ms, y `activeSince`: desde cuándo corre el tiempo activo, solo con la tarea `IN_PROGRESS`). |
| Value object | `TaskStatus` | `ON_HOLD`, `IN_PROGRESS`, `COMPLETED`, `DONE`, `EXPIRED`. |

Comportamiento del agregado:

- `updateStatus(cmd)` y `updateTask(cmd)` mantienen el reloj del tiempo activo: al salir de `IN_PROGRESS` (a `ON_HOLD`, `COMPLETED` o `EXPIRED`) suman a `timePassed` lo transcurrido desde `activeSince` (sin pasar de `dueDate`) y lo limpian; al entrar a `IN_PROGRESS` lo fijan. Las transiciones `COMPLETED`, `ON_HOLD` o `EXPIRED` → `IN_PROGRESS` incrementan `timesRearranged`.
- `settleActiveTime()`: la usa `TaskStatusScheduler` tras marcar `EXPIRED` una tarea (el `UPDATE` condicional solo cambia el estado) para sumar el tiempo hasta la fecha límite.
- `updateTask(cmd)`: actualiza los campos no vacíos; si `dueDate` es distinta de la actual, recalcula `EXPIRED` / `IN_PROGRESS`. Devuelve si cambió algún dato (título, descripción, fecha, clasificación): `TaskCommandServiceImpl` lo usa, junto con la reasignación, para resolver la `MODIFICATION` pendiente (`RequestsContextFacade.resolvePendingModification`) y reanudar la tarea (`resumeAfterEditing`).
- `applySubmissionReview(approved)`: `DONE` si se aprueba, `IN_PROGRESS` si se rechaza. Solo lo invoca requests.
- `ensureNotFinished()`: una tarea `DONE` es definitiva; editarla, cambiarle el estado o borrarla responde 409 (`TaskFinishedException`, AUDIT BUG-18).

## Capa de aplicación

| Servicio | Mensajes |
|---|---|
| `TaskCommandServiceImpl` | `CreateTaskCommand`, `UpdateTaskCommand` (incluye reasignación entre membresías), `DeleteTaskCommand`, `UpdateTaskStatusCommand` y `DeleteTasksByUserIdAndGroupIdCommand`. |
| `TaskQueryServiceImpl` | `GetTaskByIdQuery`, `GetAllTasksByUserId`, `GetAllTasksByGroupIdQuery`, `GetTasksByGroupIdAndStatus`, `GetTasksByGroupIdAndUserIdQuery`. |
| `TaskStatusScheduler` (`application/internal/schedulers`) | `@Scheduled` cada `app.tasks.expiration-check-ms` (60 s; variable `TASKS_EXPIRATION_CHECK_MS`): busca las tareas `IN_PROGRESS` vencidas y, en una transacción por tarea, las marca con un `UPDATE` condicional (`TaskRepository.markExpiredIfInProgress`, `WHERE status = IN_PROGRESS`). Solo si cambió la fila crea `CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", taskId)` sin `requesterId`, por lo que se salta las validaciones de usuario. Con varias instancias, solo una crea la request (AUDIT BUG-09). |

## ACL

Hacia requests: `RequestsContextFacade.deleteAllRequestsByTaskId`, que `DeleteTaskCommand` y `DeleteTasksByUserIdAndGroupIdCommand` llaman antes de borrar cada tarea. El borrado es lógico (HUM-03): `Task` y `Request` se marcan con `deleted_at`, dejan de ser visibles y la tarea conserva su perfil (responsable y grupo).

Hacia groups e IAM: `GroupsContextFacade` (`fetchLeaderIdByGroupId`, `isLeader`, `fetchMembership`) e `IamContextFacade.fetchUserById`; no usa sus repositorios (AUDIT ARC-01).

Hacia otros contextos:

`TasksContextFacade` (`interfaces/acl`) / `TaskContextFacadeImpl` (`application/internal/acl`):

| Método | Lo usa |
|---|---|
| `getTaskById` | requests (crear y eliminar solicitudes), metrics (métricas de una tarea) |
| `fetchTasksByGroupId`, `fetchTasksByGroupIdAndUserId` | metrics |
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

Sin observaciones abiertas: los hallazgos de esta parte están resueltos (ver [AUDITORIA.md](../../../AUDITORIA.md)).

