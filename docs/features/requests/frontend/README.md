# Requests: frontend

Carpeta: [`frontend/src/app/requests`](../../../../frontend/src/app/requests)

## Rutas y vistas

| Ruta | Vista | Stores | Qué hace |
|---|---|---|---|
| `/validations?groupId=` | `Validations` (`presentation/views/validations`) | `RequestsStore`, `GroupsStore` | Bandeja del líder: elige uno de sus grupos (`leaderGroups`), filtra por estado (por defecto `PENDING`) y aprueba o rechaza entregas. |

La otra cara de la feature está en un componente de tasks: **`TaskCard`** usa `RequestsStore` para que el miembro envíe la entrega de una tarea `COMPLETED` y vea si ya está "en revisión".

## Estado: `RequestsStore` (`application/requests.store.ts`)

| Signal | Contenido |
|---|---|
| `submissions` | Entregas del grupo seleccionado (bandeja del líder). |
| `requestsByTask` | `Record<taskId, TaskRequest[]>` para las tarjetas. |
| `statusFilter` | `RequestStatus`, por defecto `PENDING`. |
| `loading`, `processing`, `error` | Estado de la UI. |

| Método | Petición |
|---|---|
| `loadGroupSubmissions(groupId, status?)` | `GET /groups/{groupId}/requests?status=…&type=SUBMISSION` |
| `approveSubmission(request)` / `rejectSubmission(request)` | `PUT /tasks/{taskId}/requests/{id}/status/APPROVED\|REJECTED` |
| `submitTask(taskId, description, onSuccess?)` | `POST /tasks/{taskId}/requests` con `requestType: 'SUBMISSION'` |
| `loadTaskRequests(taskId)` | `GET /tasks/{taskId}/requests` |
| `isUnderReview(taskId)` | Sin petición: busca en `requestsByTask` una `SUBMISSION` en `PENDING`. |

## Infraestructura

| Archivo | Rol |
|---|---|
| `requests.api.ts` (`RequestsApi`) | Fachada: `createRequest`, `getRequestsByTask`, `getRequestsByGroup`, `approveRequest` y `rejectRequest`. |
| `requests.api-endpoint.ts` | Extiende `BaseApiEndpoint` con base `/tasks` y arma las rutas anidadas: `/tasks/{id}/requests…` y `/groups/{id}/requests`. Traduce 403, 404 y 409 a mensajes en español según la operación. |
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
