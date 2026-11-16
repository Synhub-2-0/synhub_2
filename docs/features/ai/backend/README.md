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
| Queries | `ClassifyTaskQuery(title, description, dueDate)`, `GenerateGroupReportQuery(groupId, requesterId)` |
| Servicios | `TaskClassificationQueryService`, `GroupReportQueryService` |

## Implementación

### `TaskClassificationQueryServiceImpl`

- Llama a Bedrock `Converse` con un *system prompt* en español y **fuerza el uso de la herramienta** `classify_task` (`ToolChoice.tool`), cuyo JSON Schema exige `labels`, `urgency`, `difficulty`, `context` y `rationale`.
- `temperature = 0` y `maxTokens = 400`. El mensaje incluye la fecha actual para que el modelo razone sobre la urgencia.
- Normaliza la respuesta: `rationale` de hasta 500 caracteres (lo que cabe en la tarea), como máximo 3 etiquetas, `urgency` ∈ {`LOW`, `MEDIUM`, `HIGH`} (si no, `MEDIUM`), `difficulty` acotada a 1-5, y `context` ∈ {`FRONTEND`, `BACKEND`, `DATABASE`, `CLOUD`, `QA`, `DOCS`, `DESIGN`, `GENERAL`} (si no, `GENERAL`).
- Ante cualquier `RuntimeException` devuelve `Optional.empty()` y el controlador responde 503.

### `GroupReportQueryServiceImpl`

1. Arma un texto de "hechos" con:
   - `TaskMetricsQueryService` ([metrics](../../metrics/backend/README.md)): tareas por estado y reagendados del grupo.
   - `TaskQueryService` (tasks): por cada integrante, tareas abiertas, **carga ponderada** (suma de `difficulty`; 3 si no tiene), vencidas, reagendados, tareas que vencen en las próximas 48 h y tareas abiertas de urgencia `HIGH`.
   - `GroupQueryService` (groups): **todos** los integrantes, también los que no tienen tareas (aparecen con ceros), para que el informe pueda proponer a quién reasignar.
2. Le pide al modelo un informe con las secciones *Resumen*, *Carga del equipo*, *Riesgos* y *Recomendaciones* (`temperature = 0.2`, `maxTokens = 900`). El prompt le pide empezar en *Resumen* (sin título general ni fecha, porque el servidor no conoce la zona horaria del usuario), no usar tablas (la web no las renderiza) y no pedir más datos ni sugerir revisar el sistema.
3. Si el grupo no tiene tareas, responde un texto fijo **sin llamar al modelo**.
4. Ante un error devuelve vacío y el controlador responde 503.

### `BedrockConfiguration`

Crea el `BedrockRuntimeClient` para la región configurada en `ai.bedrock.region`, con un límite de 20 s por intento y 30 s por llamada: si Bedrock no responde, el usuario recibe el 503 en lugar de quedarse esperando.

## Dependencias

| Hacia | Mediante |
|---|---|
| iam | `UserQueryService` (usuario del token) |
| groups | `GroupQueryService` (validar al líder y listar a los integrantes) |
| tasks | `TaskQueryService` y el agregado `Task` |
| metrics | `TaskMetricsQueryService` |

Todas son inyecciones directas de servicios de otros contextos, no ACL.

## Pruebas

- `TaskClassificationQueryServiceImplTest`: mapeo de una respuesta válida, normalización de valores fuera de rango y respuesta vacía si Bedrock falla (con el cliente simulado).
- `GroupReportQueryServiceImplTest`: hechos enviados al modelo (incluye a los integrantes sin tareas y la urgencia alta), reglas del prompt, grupo sin tareas sin llamar al modelo y fallo de Bedrock.
- `api/AiApiTests`: sesión, validación de `/ai/classify`, permisos del informe y grupo sin tareas, por HTTP.
- `TaskClassificationQueryServiceImplIT`: llama a Bedrock de verdad; solo corre con `RUN_BEDROCK_IT=1` y credenciales de AWS.
