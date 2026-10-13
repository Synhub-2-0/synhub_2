# Metrics: backend

Paquete: [`com.hub.synhub_backend.metrics`](../../../../backend/src/main/java/com/hub/synhub_backend/metrics)

## Endpoints (`MetricsController`)

Implementación base (decisión HUM-05). Todos son `GET`, requieren sesión y son de solo lectura. El control de acceso usa las ACL de groups (`GroupsContextFacade.isMember` / `isLeader`) y de tasks (`TasksContextFacade.getTaskById`).

| Ruta | Respuesta | Quién puede verla |
|---|---|---|
| `/api/v1/metrics/groups/{groupId}/overview` | `TaskOverviewResource` | Integrantes del grupo |
| `/api/v1/metrics/groups/{groupId}/distribution` | `TaskDistributionResource` | Integrantes del grupo |
| `/api/v1/metrics/groups/{groupId}/rescheduled` | `RescheduledTasksResource` | Integrantes del grupo |
| `/api/v1/metrics/groups/{groupId}/avg-completion-time` | `AvgCompletionTimeResource` | Integrantes del grupo |
| `/api/v1/metrics/groups/{groupId}/members/{userId}/overview` | `TaskOverviewResource` | El líder o el propio integrante |
| `/api/v1/metrics/groups/{groupId}/members/{userId}/distribution` | `TaskDistributionResource` | El líder o el propio integrante |
| `/api/v1/metrics/groups/{groupId}/members/{userId}/rescheduled` | `RescheduledTasksResource` | El líder o el propio integrante |
| `/api/v1/metrics/groups/{groupId}/members/{userId}/avg-completion-time` | `AvgCompletionTimeResource` | El líder o el propio integrante |
| `/api/v1/metrics/groups/{groupId}/members/{userId}/time-passed` | `TaskTimePassedResource` | El líder o el propio integrante |
| `/api/v1/metrics/tasks/{taskId}/time-passed` | `TaskTimePassedResource` · `409` si la tarea no está `COMPLETED`/`DONE` | Integrantes del grupo de la tarea |
| `/api/v1/metrics/tasks/{taskId}/duration` | `TaskDurationResource` · `409` si la tarea no está `IN_PROGRESS` | Integrantes del grupo de la tarea |

Errores: `404` grupo, tarea o integrante inexistente; `403` sin permiso; `409` métrica no aplicable al estado de la tarea (`MetricNotAvailableException`).

Recursos:

- `TaskOverviewResource { type, value: total, details: { estado: n } }`
- `TaskDistributionResource { type, value: total, details: { userId: { memberName, taskCount } } }`
- `RescheduledTasksResource { type, value: total de reagendados, details: { total, rescheduled }, rescheduledMemberIds }`
- `AvgCompletionTimeResource { type, value: días promedio de las tareas DONE, details: { completedTasks } }`
- `TaskTimePassedResource { memberId, timePassed (ms) }`
- `TaskDurationResource { taskId, durationInHours }` (de la creación a la fecha límite)

## Consultas (`TaskMetricsQueryService` / `TaskMetricsQueryServiceImpl`)

| Query | Fuente | Consumidores |
|---|---|---|
| `GetTaskOverviewInGroupQuery(groupId)` | `findByProfile_Group_Id` | `MetricsController`, informe de IA |
| `GetRescheduledTasksInGroupQuery(groupId)` | `findByProfile_Group_Id` | `MetricsController`, informe de IA |
| `GetTaskDistributionInGroupQuery(groupId)` | `findByProfile_Group_Id` (el nombre sale del perfil de la tarea) | `MetricsController` |
| `GetAvgCompletionTimeInGroupQuery(groupId)` | `GroupQueryService` + `findByProfile_Group_Id` | `MetricsController` |
| `GetTaskOverviewForUserInGroupQuery(memberId, groupId)`, `GetTaskDistributionForUserInGroupQuery`, `GetRescheduledTasksForUserInGroupQuery`, `GetAvgCompletionTimeForUserInGroupQuery`, `GetTaskTimePassedForUserInGroupQuery(userId, groupId)` | `findByProfile_Group_IdAndProfile_User_Id`: **solo el grupo pedido** (AUDIT BUG-08) | `MetricsController` |
| `GetTaskTimePassedByIdQuery(taskId)`, `GetTaskDurationByIdQuery(taskId)` | `findById` | `MetricsController` |

Las tareas borradas (soft delete, HUM-03) no cuentan en ninguna métrica: el repositorio las excluye.

## Modelo

Metrics no tiene modelo propio ni persistencia: calcula sobre las tareas de tasks.

## Estructura y observaciones

- **Inversión de capas**: el servicio de dominio (`domain/model/services/TaskMetricsQueryService`) devuelve tipos de `interfaces/rest/resources` (`TaskOverviewResource`, etc.). Lo habitual en el resto del backend es que el dominio devuelva entidades o value objects, y que los assemblers de `interfaces/rest/transform` los conviertan (ver ARC-02).
- Los nombres de paquete tienen erratas: `application/internal/queryservice` (sin "s") e `infrastructure/persistenence`.
- `TaskMetricsQueryService` está en `domain/model/services` en lugar de `domain/services`, que es donde lo ponen los demás contextos.
- Lee `TaskRepository` de tasks directamente (ARC-01).
- Pruebas: `metrics/TaskMetricsQueryServiceImplTest` (cálculos) y `api/MetricsApiTests` (endpoints y permisos).
