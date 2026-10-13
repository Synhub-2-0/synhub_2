# AI: frontend

Carpeta: [`frontend/src/app/ai`](../../../../frontend/src/app/ai). No tiene rutas propias: aporta un store y un componente que usan las vistas de tasks.

## Dónde se usa

| Vista (de tasks) | Elemento de ai | Qué hace |
|---|---|---|
| `TaskForm` (`/tasks/create`, `/tasks/:id/edit`) | `AiStore.classifyTask`, `classification`, `clearClassification` | Llena el formulario con la sugerencia, que se puede editar, y calcula `aiAccepted` al guardar (ver [tasks/frontend](../../tasks/frontend/README.md#integración-con-ia-en-taskform)). |
| `LeaderTasks` (`/tasks/leader`) | `<app-ai-report-card [groupId]>` | Tarjeta con un botón para generar el informe del grupo seleccionado. |

## Estado: `AiStore` (`application/ai.store.ts`)

| Signal / método | Contenido |
|---|---|
| `classification`, `classifying`, `error` | Última sugerencia y su estado. |
| `classifyTask(payload)` | `POST /ai/classify` |
| `clearClassification()` | Limpia la sugerencia. |
| `reportFor(groupId)`, `isReportLoading(groupId)`, `reportErrorFor(groupId)` | Informes en caché **por grupo** (`Record<groupId, GroupReport>`), con su estado de carga y error. |
| `loadReport(groupId)` | `GET /ai/groups/{groupId}/report` |

## Infraestructura

`ai.api.ts` (`AiApi`) usa `HttpClient` directamente, sin `BaseApiEndpoint` ni assembler, porque las respuestas se mapean 1 a 1:

- `classifyTask({ title, description, dueDate? })`: convierte `dueDate` a ISO.
- `getGroupReport(groupId)`: devuelve solo el texto de `report`.

## Modelo (`domain/model/task-classification.entity.ts`)

| Elemento | Contenido |
|---|---|
| `TaskClassification` | `labels`, `urgency`, `difficulty`, `context`, `rationale` |
| `TaskContext`, `TASK_CONTEXTS` | `FRONTEND`, `BACKEND`, `DATABASE`, `CLOUD`, `QA`, `DOCS`, `DESIGN`, `GENERAL`: deben coincidir con el backend. |
| `TaskUrgency`, `TASK_URGENCIES` | `LOW`, `MEDIUM`, `HIGH` |
| `DIFFICULTY_SCALE` | Texto descriptivo para cada valor de 1 a 5. |
| `GroupReport` | `text`, `generatedAt` |
| `parseReport(markdown)` | Convierte el markdown mínimo del informe (títulos, listas y negritas) en bloques renderizables **sin `innerHTML`**, para evitar XSS a partir de la salida del modelo. |

## Componente `AiReportCard`

`input groupId` → `computed` sobre `AiStore` (informe, carga y error) → renderiza los bloques de `parseReport`.

## Pruebas

- `ai.store.spec.ts`
- `task-classification.entity.spec.ts` (`parseReport`)
