# Metrics: backend

Paquete: [`com.hub.synhub_backend.metrics`](../../../../backend/src/main/java/com/hub/synhub_backend/metrics)

## Endpoints

`MetricsController` está mapeado a `/api/v1/metrics`, pero **no tiene métodos**: solo TODOs que describen los endpoints previstos (resumen, distribución, reagendados, tiempo promedio, por grupo y por integrante, y duración de una tarea en curso).

## Consultas (`TaskMetricsQueryService` / `TaskMetricsQueryServiceImpl`)

| Query | Resultado | Fuente | ¿Se usa? |
|---|---|---|---|
| `GetTaskOverviewInGroupQuery(groupId)` | `TaskOverviewResource { type: "TASK_OVERVIEW", value: total, details: {estado: n} }` | `findByProfile_Group_Id` | **Sí** (informe de IA) |
| `GetRescheduledTasksInGroupQuery(groupId)` | `RescheduledTasksResource { value: total de reagendados, details, rescheduledMemberIds }` | `findByProfile_Group_Id` | **Sí** (informe de IA) |
| `GetTaskDistributionInGroupQuery(groupId)` | `TaskDistributionResource` (tareas por integrante) | `TaskRepository` + `UserRepository.findAll` | No |
| `GetAvgCompletionTimeInGroupQuery(groupId)` | `AvgCompletionTimeResource` (días promedio de las tareas `DONE`) | `GroupQueryService` + `TaskRepository` | No |
| `GetTaskOverviewForUserInGroupQuery(memberId)` | Resumen por estado del usuario | `findByProfile_User_Id` | No |
| `GetTaskDistributionForUserInGroupQuery(userId, groupId)` | — | `findByProfile_User_Id` | No |
| `GetRescheduledTasksForUserInGroupQuery(userId, groupId)` | — | `findByProfile_User_Id` | No |
| `GetAvgCompletionTimeForUserInGroupQuery(userId, groupId)` | — | `findByProfile_User_Id` | No |
| `GetTaskTimePassedForUserInGroupQuery(userId)` | `TaskTimePassedResource` (promedio de `timePassed`) | `findByProfile_User_Id` | No |
| `GetTaskTimePassedByIdQuery(taskId)` | `timePassed` de una tarea `COMPLETED`/`DONE` | `findById` | No |
| `GetTaskDurationByIdQuery(taskId)` | `TaskDurationResource` (horas entre la creación y la fecha límite, solo `IN_PROGRESS`) | `findById` | No |

Las consultas "ForUserInGroup" reciben `groupId`, pero **filtran solo por usuario** (`findByProfile_User_Id`): mezclan las tareas de todos los grupos del usuario.

## Modelo

`GroupMetrics(groupId, memberCount)`: es una clase simple, no una entidad JPA. La obtiene `GroupsClient` mediante `GroupMetricsRepository`.

## Estructura y observaciones

- **Inversión de capas**: el servicio de dominio (`domain/model/services/TaskMetricsQueryService`) devuelve tipos de `interfaces/rest/resources` (`TaskOverviewResource`, etc.). Lo habitual en el resto del backend es que el dominio devuelva entidades o value objects, y que los assemblers de `interfaces/rest/transform` los conviertan.
- Los nombres de paquete tienen erratas: `application/internal/queryservice` (sin "s") e `infrastructure/persistenence`.
- `TaskMetricsQueryService` está en `domain/model/services` en lugar de `domain/services`, que es donde lo ponen los demás contextos.
- Sin pruebas propias.
