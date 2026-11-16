# AI: frontend

Carpeta: [`frontend/src/app/ai`](../../../../frontend/src/app/ai). No tiene rutas propias: aporta un store y un componente que usan las vistas de tasks.

## Dónde se usa

| Vista (de tasks) | Elemento de ai | Qué hace |
|---|---|---|
| `TaskForm` (`/tasks/create`, `/tasks/:id/edit`) | `AiStore.classifyTask`, `classification`, `clearClassification` | Llena la clasificación con la sugerencia, que es opcional y se puede editar, volver a pedir o quitar, tanto al crear como al editar; calcula `aiAccepted` al guardar (ver [tasks/frontend](../../tasks/frontend/README.md#integración-con-ia-en-taskform)). |
| `LeaderTasks` (`/tasks/leader`) | `<app-ai-report-card [groupId]>` | Tarjeta con un botón para generar el informe del grupo seleccionado. |

## Estado: `AiStore` (`application/ai.store.ts`)

| Signal / método | Contenido |
|---|---|
| `classification`, `classifying`, `error` | Sugerencia del formulario abierto y su estado. Solo cuenta la última petición: la respuesta de una anterior, o de un formulario que ya se cerró, se descarta. |
| `classifyTask(payload)` | `POST /ai/classify` |
| `clearClassification()` | Limpia la sugerencia y descarta la petición en curso. `TaskForm` lo llama al abrirse y al cerrarse. |
| `reportFor(groupId)`, `isReportLoading(groupId)`, `reportErrorFor(groupId)` | Informes en caché **por grupo** (`Record<groupId, GroupReport>`), con su estado de carga y error. |
| `loadReport(groupId)` | `GET /ai/groups/{groupId}/report` |
| `reset()` | Vacía la sugerencia y los informes. Se ejecuta al cerrar sesión, como en los demás stores, para que la siguiente sesión no vea datos de la anterior. |

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
