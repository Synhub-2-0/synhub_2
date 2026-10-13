# AI: backend

Paquete: [`com.hub.synhub_backend.ai`](../../../../backend/src/main/java/com/hub/synhub_backend/ai). Solo tiene consultas (*queries*): no hay comandos ni repositorios.

## Endpoints (`AiController`)

Requieren token.

| Método | Ruta | Cuerpo | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/ai/classify` | `ClassifyTaskResource { title, description, dueDate }` | `200 TaskClassificationResource { labels[], urgency, difficulty, context, rationale }` · `400` si `title` está vacío · `503` si Bedrock falla | — |
| `GET` | `/api/v1/ai/groups/{groupId}/report` | — | `200 GroupReportResource { report }` (markdown) · `403` · `404` · `503` | Solo el líder del grupo. |

## Modelo

| Tipo | Clase |
|---|---|
| Value object | `TaskClassification(labels, urgency, difficulty, context, rationale)` |
| Queries | `ClassifyTaskQuery(title, description, dueDate)`, `GenerateGroupReportQuery(groupId)` |
| Servicios | `TaskClassificationQueryService`, `GroupReportQueryService` |

## Implementación

### `TaskClassificationQueryServiceImpl`

- Llama a Bedrock `Converse` con un *system prompt* en español y **fuerza el uso de la herramienta** `classify_task` (`ToolChoice.tool`), cuyo JSON Schema exige `labels`, `urgency`, `difficulty`, `context` y `rationale`.
- `temperature = 0` y `maxTokens = 400`. El mensaje incluye la fecha actual para que el modelo razone sobre la urgencia.
- Normaliza la respuesta: como máximo 3 etiquetas, `urgency` ∈ {`LOW`, `MEDIUM`, `HIGH`} (si no, `MEDIUM`), `difficulty` acotada a 1-5, y `context` ∈ {`FRONTEND`, `BACKEND`, `DATABASE`, `CLOUD`, `QA`, `DOCS`, `DESIGN`, `GENERAL`} (si no, `GENERAL`).
- Ante cualquier `RuntimeException` devuelve `Optional.empty()` y el controlador responde 503.

### `GroupReportQueryServiceImpl`

1. Arma un texto de "hechos" con:
   - `TaskMetricsQueryService` ([metrics](../../metrics/backend/README.md)): tareas por estado y reagendados del grupo.
   - `TaskQueryService` (tasks): por cada integrante, tareas abiertas, **carga ponderada** (suma de `difficulty`; 3 si no tiene), vencidas, reagendados y tareas que vencen en las próximas 48 h.
2. Le pide al modelo un informe con las secciones *Resumen*, *Carga del equipo*, *Riesgos* y *Recomendaciones* (`temperature = 0.2`, `maxTokens = 900`).
3. Ante un error devuelve vacío y el controlador responde 503.

### `BedrockConfiguration`

Crea el `BedrockRuntimeClient` para la región configurada en `ai.bedrock.region`.

## Dependencias

| Hacia | Mediante |
|---|---|
| iam | `UserQueryService` (usuario del token) |
| groups | `GroupQueryService` (validar al líder) |
| tasks | `TaskQueryService` y el agregado `Task` |
| metrics | `TaskMetricsQueryService` |

Todas son inyecciones directas de servicios de otros contextos, no ACL.

## Pruebas

- `TaskClassificationQueryServiceImplTest`: mapeo de una respuesta válida, normalización de valores fuera de rango y respuesta vacía si Bedrock falla (con el cliente simulado).
- `TaskClassificationQueryServiceImplIT`: llama a Bedrock de verdad; solo corre con `RUN_BEDROCK_IT=1` y credenciales de AWS.
