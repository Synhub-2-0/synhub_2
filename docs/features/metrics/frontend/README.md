# Metrics: frontend

Carpeta: [`frontend/src/app/metrics`](../../../../frontend/src/app/metrics).

Metrics no tiene una ruta propia. `GroupDetails` (groups) muestra distribución, desglose de carga y estado general de las tareas para el líder del grupo; la tarjeta de tareas reprogramadas también usa esta feature.

## Componentes y datos

- `MetricsStore` carga el resumen por estado, la distribución del grupo y las tareas reprogramadas mediante `MetricsApi`.
- `TaskDistributionChart` presenta la asignación por integrante como un donut con AG Charts.
- `TaskStatusOverview` muestra resumen y porcentajes por estado, con un donut y una leyenda. `DONE` es "Terminadas", `EXPIRED` "Vencidas / En riesgo", `IN_PROGRESS` "En progreso", y `ON_HOLD` más `COMPLETED` (pendiente de revisión) se agrupan como "En espera".
- `MemberWorkloadBreakdown` presenta cada integrante, las tareas asignadas, su porcentaje del total y el desglose de tareas vencidas, en curso y terminadas.
- El desglose reutiliza `TasksStore.tasks()` y `GroupsStore.membersByGroup`; no hace consultas individuales por integrante. "En curso" agrupa `IN_PROGRESS`, `ON_HOLD` y `COMPLETED`.

## Infraestructura

- `MetricsApiEndpoint` consume `GET /metrics/groups/{groupId}/overview`, `…/distribution` y `…/rescheduled`.
- Las URLs usan `environment.platformProviderMetricsEndpointPath`.
- El informe de IA consume las métricas desde el backend; no pasa por este store. Ver [ai/frontend](../../ai/frontend/README.md).
