# Tasks: tareas de un grupo

El líder crea tareas dentro de su grupo y se las asigna a un integrante. El integrante avanza la tarea por un tablero Kanban; al completarla envía una **entrega**, que el líder revisa (feature [requests](../requests/README.md)). Una tarea solo pasa a `DONE` cuando el líder aprueba esa entrega.

| | |
|---|---|
| **Recurso REST** | `/api/v1/tasks` |
| **Bounded context (backend)** | [`tasks`](../../../backend/src/main/java/com/hub/synhub_backend/tasks) |
| **Módulo (frontend)** | [`src/app/tasks`](../../../frontend/src/app/tasks) |
| **Rutas de la web** | `/tasks/leader`, `/tasks/member`, `/tasks/create`, `/tasks/:id`, `/tasks/:id/edit`, además del tablero en `/home` y `/groups/:id` |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Ciclo de vida de una tarea

```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS: crear (líder)
    IN_PROGRESS --> ON_HOLD: PUT /status (líder) o el integrante pide una modificación (POST /requests)
    ON_HOLD --> IN_PROGRESS: PUT /status (líder, +1 reagendado) o el líder edita la tarea con una modificación pendiente
    IN_PROGRESS --> COMPLETED: el integrante envía su entrega (POST /requests) o PUT /status (líder); suma el tiempo activo a timePassed
    COMPLETED --> IN_PROGRESS: PUT /status (líder) o entrega rechazada (+1 reagendado)
    COMPLETED --> DONE: entrega aprobada (solo vía requests)
    IN_PROGRESS --> EXPIRED: scheduler, fecha límite vencida (crea request EXPIRED)
    EXPIRED --> IN_PROGRESS: PUT /status (líder, +1 reagendado)
    DONE --> [*]
```

- `PUT /tasks/{id}/status` acepta cualquier estado **excepto `DONE`** (responde 409 `TaskStatusTransitionNotAllowedException`). **Solo puede hacerlo el líder** del grupo (403 para el integrante asignado); el integrante completa su tarea enviando la entrega (`POST /requests`), que la pasa a `COMPLETED`.
- `PUT /tasks/{id}` con una `dueDate` **distinta** de la actual y pasada mueve la tarea a `EXPIRED`; con una futura la vuelve a `IN_PROGRESS`, salvo que ya esté `EXPIRED`. Una fecha igual a la actual no cambia el estado (la web siempre la envía al editar).
- **Tiempo activo (`timePassed`)**: solo corre mientras la tarea está `IN_PROGRESS` (`Task.activeSince`, migración `V3`). Al salir de ese estado (espera, completada, vencida) se suma lo transcurrido; al volver, el reloj empieza de nuevo. Una tarea vencida cuenta solo hasta su fecha límite.
- **Modificación pendiente**: si el integrante pidió una modificación, la tarea queda `ON_HOLD` y `PUT /tasks/{id}/status` a `IN_PROGRESS` responde 409. Se reanuda cuando el líder la edita (cambiar título, descripción, fecha límite, clasificación o responsable): esa edición cierra la solicitud (`APPROVED`).
- `TaskStatusScheduler` revisa cada 60 s las tareas `IN_PROGRESS` vencidas: las marca `EXPIRED` y crea una solicitud `EXPIRED` en requests.
- En la web, una tarea `DONE` no se puede editar ni eliminar (`TasksStore` lo bloquea).

## Comunicación frontend ↔ backend

| Acción en la web | Frontend | Petición | Backend |
|---|---|---|---|
| Crear tarea (líder) | `TaskForm` → `TasksStore.addTask` | `POST /tasks` | `TaskController.createTask` → `CreateTaskCommand` |
| Sugerir clasificación con IA | `TaskForm` → `AiStore.classifyTask` | `POST /ai/classify` | [ai](../ai/README.md) |
| Editar tarea (líder) | `TaskForm` → `TasksStore.updateTask` | `PUT /tasks/{id}` | `updateTaskDetails` → `UpdateTaskCommand` |
| Ver detalle | `TaskDetails`, `TaskForm` (edición) → `loadTaskById` | `GET /tasks/{id}` | `getTaskById` |
| Tablero del grupo | `Home`, `GroupDetails`, `LeaderTasks` → `loadTasksByGroup` | `GET /tasks/group/{groupId}[?status=]` | `getByGroup` → `GetTasksByGroupIdAndStatus` |
| Mis tareas | `MemberTasks` → `loadMyTasks` | `GET /tasks/user` (una sola petición) | `getMyTasks` → `GetAllTasksByUserId` |
| Mover de columna (solo el líder) | `TaskBoard` (`statusChange`) → `updateTaskStatus` | `PUT /tasks/{id}/status?status=` | `updateTaskStatus` → `UpdateTaskStatusCommand` |
| Eliminar (líder) | `GroupDetails`, `LeaderTasks` → `deleteTask` | `DELETE /tasks/{id}` | `deleteTaskById` → `DeleteTaskCommand` |
| Enviar entrega (miembro) | `TaskCard` → `RequestsStore.submitTask` | `POST /requests` | [requests](../requests/README.md) |

## Propiedad compartida

- **Asignación mediante groups**: `Task.profile` apunta a `GroupProfile` (la membresía de groups) y `GroupProfile.tasks` es la colección inversa. Una tarea no tiene `userId` ni `groupId` propios: ambos se derivan de la membresía. `TaskResource` expone `group` y `assignedTo` armados con assemblers de groups e IAM.
- **`TaskCommandServiceImpl` usa las ACL de groups e IAM** (`GroupsContextFacade`, `IamContextFacade`) para el líder, la membresía y el usuario (AUDIT ARC-01).
- **Scheduler de vencimiento**: `TaskStatusScheduler` está en `tasks/application/internal/schedulers`, pero escribe en **requests** (`RequestCommandService`, solicitud `EXPIRED`). El proceso es de tasks y el dato resultante es de requests.
- **`DONE` lo decide requests**: requests llama a `TasksContextFacade.applySubmissionReview` al revisar una entrega. Es la única vía hacia `DONE`.
- **Campos de IA en `Task`** (`difficulty`, `context`, `urgency`, `labels`, `aiAccepted`): se persisten en tasks, pero los produce [ai](../ai/README.md) y los consume su informe de grupo.
- **Métricas**: `TaskMetricsQueryServiceImpl` ([metrics](../metrics/README.md)) lee las tareas por `TasksContextFacade`.
- **Frontend**: `TaskCard` (tasks) usa `RequestsStore`; `TaskForm` usa `AiStore` y `GroupsStore`; `LeaderTasks` incrusta `AiReportCard` (ai); `Home` (shared) y `GroupDetails` (groups) usan `TasksStore`.

Más detalle en [Propiedad entre features](../../architecture/cross-feature-ownership.md).

## Grupos bloqueados

Crear, editar, borrar y cambiar el estado de una tarea responden 409 (`GroupLockedException`) si su grupo está bloqueado por el plan de su líder (se comprueba después de los permisos). Las lecturas siguen; el **vencimiento automático** (`TaskStatusScheduler`) sigue marcando `EXPIRED` las tareas vencidas del grupo. En la web, `TaskCard`, `TaskBoard`, `LeaderTasks` y `TaskMemberActions` no ofrecen acciones en un grupo bloqueado. Ver [subscriptions](../subscriptions/README.md).
