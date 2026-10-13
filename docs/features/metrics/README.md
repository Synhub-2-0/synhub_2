# Metrics: métricas de productividad

Cálculos sobre las tareas de un grupo o de un integrante: distribución por estado, reparto entre integrantes, reagendados, tiempo promedio de cierre y duración de una tarea.

| | |
|---|---|
| **Recurso REST** | `/api/v1/metrics`: implementación base de solo lectura (HUM-05) |
| **Bounded context (backend)** | [`metrics`](../../../backend/src/main/java/com/hub/synhub_backend/metrics) |
| **Módulo (frontend)** | No existe |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Estado actual

Las 11 consultas se exponen en `/api/v1/metrics` (decisión HUM-05): métricas del grupo para sus integrantes, métricas de un integrante para el líder o para él mismo, y métricas de una tarea para los integrantes de su grupo. Ver [backend](backend/README.md). El informe de IA (`GroupReportQueryServiceImpl`, feature [ai](../ai/README.md)) también usa dos de ellas: resumen por estado y reagendados del grupo.

## Comunicación frontend ↔ backend

La web todavía no consume `/api/v1/metrics`: ve las métricas de forma indirecta, dentro del texto del informe de IA (`GET /ai/groups/{groupId}/report`). La API ya está lista para una vista de métricas.

## Propiedad compartida

- **Los datos son de tasks**: metrics no persiste nada. `TaskMetricsQueryServiceImpl` lee `TaskRepository` (tasks) directamente y `GroupQueryService` (groups) para el promedio de cierre. El control de acceso de `MetricsController` usa las ACL de groups y tasks.
- **Los campos que mide** (`timePassed`, `timesRearranged`) los calcula el agregado `Task` al cambiar de estado: si se cambia cómo se calculan, el cambio va en tasks.
- `GroupsClient` (en `metrics/infrastructure/client`) implementa `GroupMetricsRepository` consultando groups. Es un adaptador hacia otro contexto, aunque está registrado como `@Repository`.
