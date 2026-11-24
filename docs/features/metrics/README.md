# Metrics: métricas de productividad

Cálculos sobre las tareas de un grupo o de un integrante: distribución por estado, reparto entre integrantes, reagendados, tiempo promedio de cierre y duración de una tarea.

| | |
|---|---|
| **Recurso REST** | `/api/v1/metrics`: implementación base de solo lectura (HUM-05) |
| **Bounded context (backend)** | [`metrics`](../../../backend/src/main/java/com/hub/synhub_backend/metrics) |
| **Módulo (frontend)** | [`src/app/metrics`](../../../frontend/src/app/metrics) |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Estado actual

Las 11 consultas se exponen en `/api/v1/metrics` (decisión HUM-05): métricas del grupo para sus integrantes, métricas de un integrante para el líder o para él mismo, y métricas de una tarea para los integrantes de su grupo. Ver [backend](backend/README.md). El informe de IA (`GroupReportQueryServiceImpl`, feature [ai](../ai/README.md)) usa el resumen por estado y las tareas reagendadas del grupo. En detalles del grupo, el líder ve la distribución de tareas en un donut y el desglose de carga por integrante.

## Comunicación frontend ↔ backend

En detalles del grupo, `GroupDetails` usa `MetricsStore` para consultar la distribución (`GET /metrics/groups/{groupId}/distribution`) y presenta el reparto con AG Charts. Debajo, el desglose incluye a todos los integrantes, su cantidad asignada y porcentaje sobre el total, además de tareas vencidas, en curso y terminadas. Los estados `IN_PROGRESS`, `ON_HOLD` y `COMPLETED` se agrupan como "en curso"; el desglose usa las tareas del grupo ya cargadas por `TasksStore`, sin hacer una petición por integrante.

## Propiedad compartida

- **Los datos son de tasks**: metrics no persiste nada. `TaskMetricsQueryServiceImpl` lee las tareas por `TasksContextFacade` (tasks) y `GroupQueryService` (groups) para el promedio de cierre. El control de acceso de `MetricsController` usa las ACL de groups y tasks.
- **Los campos que mide** (`timePassed`, `timesRearranged`) los calcula el agregado `Task` al cambiar de estado: si se cambia cómo se calculan, el cambio va en tasks.
- **El desglose de carga** pertenece a metrics en la presentación y consume tareas cargadas por `TasksStore` (tasks) junto con los integrantes cargados por `GroupsStore` (groups).
