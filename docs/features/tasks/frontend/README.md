# Tasks: frontend

Carpeta: [`frontend/src/app/tasks`](../../../../frontend/src/app/tasks)

## Rutas y vistas

`tasks.routes.ts`, cargado bajo `/tasks` con `iamGuard`.

| Ruta | Vista | Stores | Qué hace |
|---|---|---|---|
| `/tasks/leader?groupId=` | `LeaderTasks` | `TasksStore`, `GroupsStore` | Tareas de un grupo que lidero, con filtro de estado, cambio de estado y eliminación. Incrusta `AiReportCard` ([ai](../../ai/frontend/README.md)). |
| `/tasks/member?groupId=` | `MemberTasks` | `TasksStore`, `GroupsStore`, `IamStore` | Mis tareas en uno o en todos mis grupos: una sola petición (`loadMyTasks`) y el grupo y el estado se filtran en el cliente. |
| `/tasks/create?groupId=` | `TaskForm` | `TasksStore`, `GroupsStore`, `IamStore`, `AiStore`, `ToastStore` | Alta de una tarea: elegir grupo e integrante; botón "sugerir con IA". |
| `/tasks/:id/edit` | `TaskForm` | ídem | Edición (`updateTask`). |
| `/tasks/:id` | `TaskDetails` | `TasksStore` | Detalle de solo lectura. |
| `/tasks` | — | — | Redirige a `member`. |

| Componente | Uso |
|---|---|
| `TaskBoard` | Tablero Kanban por estado. Emite `statusChange` al cambiar el `<select>` de una tarjeta. Se usa en `Home` (shared) y en `GroupDetails` (groups). |
| `TaskList` → `TaskCard` | Lista de tarjetas. `TaskCard` permite al miembro **enviar la entrega** de una tarea `COMPLETED` y muestra si ya está "en revisión" (usa `RequestsStore`). |

## Estado: `TasksStore` (`application/tasks.store.ts`)

| Signal | Contenido |
|---|---|
| `tasks`, `selectedTask` | Tareas cargadas y tarea abierta. |
| `statusFilter`, `filteredTasks` | Filtro `TaskStatus \| 'ALL'` y el `computed` resultante. |
| `assignedGroupIds` | Grupos donde el usuario tiene al menos una tarea (para `MemberTasks`); sale de `loadMyTasks`. |
| `loading`, `error`, `taskCount` | Estado de la UI. |

| Método | Petición |
|---|---|
| `loadTasksByGroup(groupId, status?)` | `GET /tasks/group/{groupId}` |
| `loadTasksByGroupAndUser(groupId, userId)` | `GET /tasks/group/{groupId}/user/{userId}` |
| `loadMyTasks()` | `GET /tasks/user`: todas mis tareas con una sola petición (AUDIT PERF-03). |
| `loadTaskById(id)` | `GET /tasks/{id}` |
| `addTask(command)` | `POST /tasks` |
| `updateTask(id, command)` | `PUT /tasks/{id}`: bloqueado en el cliente si la tarea está `DONE`. |
| `updateTaskStatus(id, status)` | `PUT /tasks/{id}/status?status=` |
| `deleteTask(id)` | `DELETE /tasks/{id}`: bloqueado en el cliente si la tarea está `DONE`. |

## Infraestructura

| Archivo | Rol |
|---|---|
| `tasks.api.ts` (`TasksApi`) | Fachada inyectable. |
| `tasks.api-endpoint.ts` | Extiende `BaseApiEndpoint` (`getById`, `delete`) y agrega `createTask`, `updateTaskDetails`, `updateTaskStatus`, `getTasksByGroup` y `getTasksByGroupAndUser`. |
| `tasks.assembler.ts` | `TaskResource` → `Task` (convierte las fechas ISO a `Date`); `CreateTaskCommand` → `CreateTaskRequest`; `UpdateTaskCommand` → `UpdateTaskRequest`. |
| `tasks.response.ts` | `TaskResource`, `CreateTaskRequest`, `UpdateTaskRequest`, `TasksResponse`. |

## Modelo

| Clase | Campos |
|---|---|
| `Task` | `id`, `title`, `description`, `dueDate`, `createdAt`, `updatedAt`, `status: TaskStatus`, `group`, `assignedTo`, `difficulty`, `context`, `urgency`, `labels`, `aiAccepted` |
| `TaskStatus` | `'ON_HOLD' \| 'IN_PROGRESS' \| 'COMPLETED' \| 'DONE' \| 'EXPIRED'` |
| `CreateTaskCommand`, `UpdateTaskCommand` | Datos del formulario. |

## Integración con IA en `TaskForm`

1. El líder escribe título, descripción y fecha límite, y pulsa "sugerir": `AiStore.classifyTask` llama a `POST /ai/classify`.
2. La sugerencia (`difficulty`, `context`, `urgency`, `labels`) llena el formulario y el líder puede editarla.
3. Al guardar, `aiAccepted` vale `true` si se guardó tal cual, `false` si se editó y `undefined` si no se usó la IA. Se persiste en la tarea para medir la aceptación.

## Pruebas

`tasks.assembler.spec.ts`.
