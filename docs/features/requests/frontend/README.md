# Requests: frontend

Carpeta: [`frontend/src/app/requests`](../../../../frontend/src/app/requests)

## Rutas y vistas

| Ruta | Vista | Stores | Qué hace |
|---|---|---|---|
| `/validations?groupId=` | `Validations` (`presentation/views/validations`) | `RequestsStore`, `GroupsStore` | Bandeja del líder: elige uno de sus grupos (`leaderGroups`), filtra por estado (por defecto `PENDING`) y aprueba o rechaza entregas. |

La otra cara de la feature está en un componente de tasks: **`TaskCard`** usa `RequestsStore` para que el miembro marque completada su tarea y la envíe a validación (el backend la pasa a `COMPLETED`), y vea si ya está "en revisión".

## Estado: `RequestsStore` (`application/requests.store.ts`)

| Signal | Contenido |
|---|---|
| `submissions` | Entregas del grupo seleccionado (bandeja del líder). |
| `requestsByTask` | `Record<taskId, TaskRequest[]>` para las tarjetas. |
| `statusFilter` | `RequestStatus`, por defecto `PENDING`. |
| `loading`, `processing`, `error` | Estado de la UI. |

| Método | Petición |
|---|---|
| `loadGroupSubmissions(groupId, status?)` | `GET /requests/group?groupId=…&status=…&type=SUBMISSION` |
| `approveSubmission(request)` / `rejectSubmission(request)` | `PUT /requests/{id}/status/APPROVED\|REJECTED` |
| `submitTask(taskId, onSuccess?)` | `POST /requests` con `{ taskId, description: 'Tarea completada.', requestType: 'SUBMISSION' }` (el miembro no escribe comentario); `onSuccess` recibe la request creada, con la tarea ya `COMPLETED` |
| `loadTaskRequests(taskId)` | `GET /requests/task?taskId=` |
| `isUnderReview(taskId)` | Sin petición: busca en `requestsByTask` una `SUBMISSION` en `PENDING`. |

## Infraestructura

| Archivo | Rol |
|---|---|
| `requests.api.ts` (`RequestsApi`) | Fachada: `createRequest`, `getRequestsByTask`, `getRequestsByGroup`, `approveRequest` y `rejectRequest`. |
| `requests.api-endpoint.ts` | Extiende `BaseApiEndpoint` con base `/requests` y arma `/requests`, `/requests/task`, `/requests/group` y `/requests/{id}/status/{status}`. Traduce 403, 404 y 409 a mensajes en español según la operación. |
| `requests.assembler.ts` | `RequestResource` → `TaskRequest`, usando `TasksAssembler` para la tarea anidada. |

## Modelo

| Clase | Campos |
|---|---|
| `TaskRequest` | `id`, `description`, `requestType`, `requestStatus`, `task: Task` (tasks) |
| `RequestType` | `'SUBMISSION' \| 'MODIFICATION' \| 'EXPIRED'` |
| `RequestStatus` | `'PENDING' \| 'APPROVED' \| 'REJECTED'` |
| `CreateRequestCommand` | `taskId`, `description`, `requestType` |

La web solo crea entregas (`SUBMISSION`). `MODIFICATION` existe en el backend y en el modelo, pero no hay UI para crearla.

## Pruebas

No hay `*.spec.ts` propios en `src/app/requests`.
