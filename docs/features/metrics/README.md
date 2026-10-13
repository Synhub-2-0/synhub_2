# Metrics: métricas de productividad

Cálculos sobre las tareas de un grupo o de un integrante: distribución por estado, reparto entre integrantes, reagendados, tiempo promedio de cierre y duración de una tarea.

| | |
|---|---|
| **Recurso REST** | `/api/v1/metrics`: **declarado, pero sin endpoints** (solo TODOs) |
| **Bounded context (backend)** | [`metrics`](../../../backend/src/main/java/com/hub/synhub_backend/metrics) |
| **Módulo (frontend)** | No existe |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Estado actual

Metrics es hoy un **servicio interno**: su único consumidor es el informe de IA (`GroupReportQueryServiceImpl`, feature [ai](../ai/README.md)), que usa dos de sus 11 consultas: resumen por estado del grupo y reagendados del grupo. El resto de las consultas está implementado, pero no se expone ni se usa.

## Comunicación frontend ↔ backend

Ninguna directa. La web ve las métricas de forma indirecta, dentro del texto del informe de IA (`GET /ai/groups/{groupId}/report`).

## Propiedad compartida

- **Los datos son de tasks**: metrics no persiste nada. `TaskMetricsQueryServiceImpl` lee `TaskRepository` (tasks) y `UserRepository` (iam) directamente, y `GroupQueryService` (groups) para el promedio de cierre.
- **Los campos que mide** (`timePassed`, `timesRearranged`) los calcula el agregado `Task` al cambiar de estado: si se cambia cómo se calculan, el cambio va en tasks.
- `GroupsClient` (en `metrics/infrastructure/client`) implementa `GroupMetricsRepository` consultando groups. Es un adaptador hacia otro contexto, aunque está registrado como `@Repository`.
