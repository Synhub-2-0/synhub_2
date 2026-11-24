# Metrics: métricas de productividad

Cálculos sobre las tareas de un grupo o de un integrante: distribución por estado, reparto entre integrantes, reagendados, tiempo promedio de cierre y duración de una tarea.

| | |
|---|---|
| **Recurso REST** | `/api/v1/metrics`: implementación base de solo lectura (HUM-05) |
| **Bounded context (backend)** | [`metrics`](../../../backend/src/main/java/com/hub/synhub_backend/metrics) |
| **Módulo (frontend)** | [`src/app/metrics`](../../../frontend/src/app/metrics) |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Estado actual

Las 11 consultas se exponen en `/api/v1/metrics` (decisión HUM-05): métricas del grupo para sus integrantes, métricas de un integrante para el líder o para él mismo, y métricas de una tarea para los integrantes de su grupo. Ver [backend](backend/README.md). El informe de IA (`GroupReportQueryServiceImpl`, feature [ai](../ai/README.md)) usa el resumen por estado y las tareas reagendadas del grupo. En detalles del grupo, el líder ve la distribución de tareas en un donut, el desglose de carga por integrante y el estado general de las tareas.

## Comunicación frontend ↔ backend

En detalles del grupo, `GroupDetails` usa `MetricsStore` para consultar distribución (`GET /metrics/groups/{groupId}/distribution`), estado general (`GET /metrics/groups/{groupId}/overview`) y tareas reprogramadas. La tarjeta de estado muestra el total, conteos y porcentajes por estado con un donut y su leyenda. `DONE` se muestra como terminada, `EXPIRED` como vencida/en riesgo, `IN_PROGRESS` como en progreso, y `ON_HOLD` junto con `COMPLETED` (pendiente de revisión) como en espera. El desglose de carga incluye a todos los integrantes, su cantidad asignada y porcentaje sobre el total, además de tareas vencidas, en curso y terminadas; reutiliza las tareas del grupo ya cargadas por `TasksStore`.

## Propiedad compartida

- **Los datos son de tasks**: metrics no persiste nada. `TaskMetricsQueryServiceImpl` lee las tareas por `TasksContextFacade` (tasks) y `GroupQueryService` (groups) para el promedio de cierre. El control de acceso de `MetricsController` usa las ACL de groups y tasks.
- **Los campos que mide** (`timePassed`, `timesRearranged`) los calcula el agregado `Task` al cambiar de estado: si se cambia cómo se calculan, el cambio va en tasks.
- **El desglose de carga** pertenece a metrics en la presentación y consume tareas cargadas por `TasksStore` (tasks) junto con los integrantes cargados por `GroupsStore` (groups).
