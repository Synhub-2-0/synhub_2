# Metrics: frontend

**No existe un módulo `metrics` en el frontend.**

La web no consume `/api/v1/metrics`, que además no tiene endpoints. Las métricas llegan al usuario solo de forma indirecta, dentro del informe generado por IA (`AiReportCard` en `/tasks/leader`, ver [ai/frontend](../../ai/frontend/README.md)).

Algunos conteos simples se calculan en el cliente a partir de las tareas ya cargadas, por ejemplo con los `computed` de `TasksStore` y `GroupsStore` (cantidad de tareas, conteos por estado en el tablero o cantidad de grupos). Esos cálculos son de [tasks](../../tasks/frontend/README.md) y [groups](../../groups/frontend/README.md), no de metrics.

## Si se agrega

Al exponer los endpoints de `MetricsController`, crear `src/app/metrics/` con la misma estructura que las demás features (`domain`, `infrastructure` con `*.api-endpoint.ts` y assembler, `application` con un `MetricsStore` y `presentation`), y registrar su ruta en `app.routes.ts` con `iamGuard`. Ver [Arquitectura del frontend](../../../architecture/frontend.md).
