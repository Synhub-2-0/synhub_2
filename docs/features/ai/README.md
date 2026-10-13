# AI: clasificación de tareas e informe de grupo

Dos funciones de IA generativa sobre **Amazon Bedrock** (modelo Claude Haiku por defecto):

1. **Clasificar una tarea** al crearla: sugiere etiquetas, urgencia, dificultad (1-5), área técnica y una justificación breve. El líder puede editar la sugerencia antes de guardar.
2. **Informe del grupo** para el líder: un resumen en markdown con la carga del equipo, los riesgos y recomendaciones, calculado con datos reales de las tareas.

| | |
|---|---|
| **Recurso REST** | `/api/v1/ai` |
| **Bounded context (backend)** | [`ai`](../../../backend/src/main/java/com/hub/synhub_backend/ai): sin persistencia propia |
| **Módulo (frontend)** | [`src/app/ai`](../../../frontend/src/app/ai) |
| **Dónde aparece en la web** | `TaskForm` (`/tasks/create`, `/tasks/:id/edit`) y `AiReportCard` en `/tasks/leader` |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Comunicación frontend ↔ backend

| Acción en la web | Frontend | Petición | Backend |
|---|---|---|---|
| "Sugerir con IA" en el formulario de tarea | `TaskForm` → `AiStore.classifyTask` | `POST /ai/classify { title, description, dueDate }` | `AiController.classifyTask` → `TaskClassificationQueryService` → Bedrock (tool use) |
| Generar el informe del grupo (líder) | `AiReportCard` → `AiStore.loadReport(groupId)` | `GET /ai/groups/{groupId}/report` | `AiController.getGroupReport` → `GroupReportQueryService` → métricas, tareas y Bedrock |

Si Bedrock falla o no hay credenciales, el backend responde **503** y la web muestra el error sin bloquear el resto del flujo: la tarea se puede guardar sin sugerencia.

## Propiedad compartida

AI es un contexto **orquestador**: no tiene datos propios y consume los de otras features.

| Dato | Dueño | Cómo lo usa ai |
|---|---|---|
| La clasificación ya guardada (`difficulty`, `context`, `urgency`, `labels`, `aiAccepted`) | **tasks**: columnas de `Task` | ai solo la *sugiere*; quien la persiste es `POST /tasks`. Para cambiar las columnas se modifica tasks; para cambiar el prompt o los valores válidos, ai. |
| Validación "solo el líder ve el informe" | **groups** (`Group.getLeader`) | `AiController` consulta `GroupQueryService` y `UserQueryService` directamente. |
| Cifras del informe | **metrics** + **tasks** | `GroupReportQueryServiceImpl` usa `TaskMetricsQueryService` (resumen por estado y reagendados) y `TaskQueryService` (tareas del grupo). |
| Lista de áreas (`FRONTEND`, `BACKEND`, …) y urgencias | **ai** | Están duplicadas en `TaskClassificationQueryServiceImpl` (backend) y en `task-classification.entity.ts` (frontend); hay que mantenerlas sincronizadas. |

En el frontend, `AiReportCard` se incrusta en `LeaderTasks` (tasks) y `TaskForm` (tasks) inyecta `AiStore`.

## Configuración

| Variable | Propiedad | Por defecto |
|---|---|---|
| `AWS_REGION` | `ai.bedrock.region` | `us-east-1` |
| `BEDROCK_MODEL_ID` | `ai.bedrock.model-id` | `us.anthropic.claude-haiku-4-5-20251001-v1:0` |
| Credenciales de AWS | Cadena por defecto del SDK (variables de entorno, perfil o rol de ECS) | — |
