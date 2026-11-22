# Requests: backend

Paquete: [`com.hub.synhub_backend.requests`](../../../../backend/src/main/java/com/hub/synhub_backend/requests)

## Endpoints

Todos requieren token.

### `RequestController`: `/api/v1/requests`

Desde el commit `727054f` las solicitudes son un recurso raíz: la tarea o el grupo se indican con `taskId` / `groupId`, ya no van en la ruta.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/requests` | `CreateRequestResource { taskId, description, requestType }` | `201 RequestResource` | Ver "Reglas al crear". |
| `GET` | `/requests/task?taskId=` | — | `200 RequestResource[]` · `403` · `404` | Solo integrantes del grupo de la tarea. |
| `GET` | `/requests/{requestId}` | — | `200` · `404` | Solo integrantes del grupo de la tarea (403). |
| `PUT` | `/requests/{requestId}/status/{status}` | `status` = `APPROVED` \| `REJECTED` | `200 RequestResource` | Ver "Reglas al revisar". |
| `DELETE` | `/requests/{requestId}` | — | `204` · `403` · `404` · `409` | El líder del grupo borra cualquiera; el integrante asignado, solo las suyas en `PENDING` que no sean `EXPIRED`. Una `MODIFICATION` pendiente no se borra (409): se resuelve editando la tarea. |
| `GET` | `/requests/group?groupId=` | `&status=` y `&type=` (opcionales; no distinguen mayúsculas); `&page=&size=` (opcionales) | `200 RequestResource[]`, de la más nueva a la más antigua, o `PageResource<RequestResource>` si se pide página. El filtro y el orden se hacen en SQL (`RequestRepository.findByGroupFiltered`) | Solo el líder del grupo (403). 404 si el grupo no existe. 400 si el filtro tiene un valor desconocido. |

`RequestResource = { id, description, requestType, requestStatus, task: TaskResource }`

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Request` (tabla `requests`) | `description`, `requestType`, `requestStatus` (se crea `PENDING`) y `task` (`@ManyToOne` hacia el `Task` de tasks). Los getters de tipo y estado devuelven `String`. |
| Value object | `RequestType` | `SUBMISSION`, `MODIFICATION`, `EXPIRED`. `fromString` no distingue mayúsculas. |
| Value object | `RequestStatus` | `PENDING`, `APPROVED`, `REJECTED`. |

## Reglas (`RequestCommandServiceImpl`)

**Al crear** (`CreateRequestCommand(description, requestType, taskId, requesterId)`):

1. Si el tipo no es válido → 400. Si la tarea no existe (`TasksContextFacade.getTaskById`) → 404.
2. Si `requesterId != null` (lo crea un usuario):
   - `EXPIRED` → 400: solo lo genera el sistema.
   - Solo el integrante asignado a la tarea → si no, 403.
   - `SUBMISSION`: la tarea debe estar `IN_PROGRESS` o `COMPLETED` (409 `TaskWithIncorrectStatusException`; `ON_HOLD` y `EXPIRED` las reanuda el líder). Si estaba `IN_PROGRESS`, la entrega **la pasa a `COMPLETED`** (`TasksContextFacade.completeForSubmission`): el integrante no cambia estados por su cuenta. No puede existir otra request en `PENDING` para la tarea (409 `PendingRequestAlreadyExistsException`).
   - `MODIFICATION`: exige un comentario (`description` no vacía, 400) y la tarea `IN_PROGRESS` (409). La pasa a `ON_HOLD` (`TasksContextFacade.putOnHoldForModification`): su reloj se detiene hasta que el líder la edite.
3. Si `requesterId == null` (lo crea el sistema, desde `TaskStatusScheduler`), se omiten las validaciones de usuario.

**Al revisar** (`UpdateRequestCommand`):

1. La request debe existir (404).
2. Solo el líder del grupo de la tarea (403).
3. Nuevo estado `PENDING` → 400. Si la request ya fue revisada → 409 `RequestAlreadyReviewedException`.
4. Una `MODIFICATION` no se revisa: responde 409 `ModificationResolvedByEditingException` (para el líder; el integrante ya recibe 403). Se resuelve cuando el líder edita la tarea: `TaskCommandServiceImpl` llama a `RequestsContextFacade.resolvePendingModification` y la request queda `APPROVED`.
5. Si es `SUBMISSION`:
   - `APPROVED`: exige la tarea en `COMPLETED` (409 si no lo está) y llama a `applySubmissionReview(taskId, true)` → `DONE`.
   - `REJECTED`: si la tarea sigue `COMPLETED`, llama a `applySubmissionReview(taskId, false)` → `IN_PROGRESS`; si no, solo cierra la request.
6. `EXPIRED` no modifica la tarea; solo cambia el estado de la request.

`RequestQueryServiceImpl` maneja `GetRequestsByTaskIdQuery` y `GetRequestByIdQuery` (solo integrantes del grupo de la tarea, vía `TasksContextFacade` + `GroupsContextFacade.isMember`), y `GetRequestsByGroupIdQuery` (valida al líder con `GroupsContextFacade`; filtra y ordena en SQL, también paginado).

## Dependencias

| Hacia | Mediante | Para |
|---|---|---|
| tasks | `TasksContextFacade` (ACL) | Cargar la tarea y aplicar el resultado de la revisión. |
| groups | `GroupsContextFacade` (ACL) | Saber quién es el líder del grupo y si el solicitante es integrante. |

Requests también **ofrece** una ACL: `RequestsContextFacade.deleteAllRequestsByTaskId` (usada por tasks antes de borrar una tarea), `hasPendingModification` (tasks la usa para rechazar con 409 reanudar a mano una tarea con una modificación pendiente) y `resolvePendingModification` (tasks la llama al editar la tarea: aprueba la modificación pendiente). Su implementación usa `RequestRepository` directamente para no formar un ciclo de beans con `RequestCommandService` → `TasksContextFacade`.
| iam | `UserQueryService` (en los controladores) | Resolver el usuario del token. |

## Persistencia

`Request` usa borrado lógico (`deleted_at`, HUM-03).

`RequestRepository`: `findByTaskId`, `deleteByTaskId`, `findByTask_Profile_Group_Id` y `existsByTask_IdAndRequestTypeAndRequestStatus` (la usa `hasPendingModification`).

## Errores

| Excepción | HTTP |
|---|---|
| `IllegalArgumentException` | 400 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `TaskWithIncorrectStatusException`, `PendingRequestAlreadyExistsException`, `RequestAlreadyReviewedException`, `ModificationResolvedByEditingException` | 409 |

## Pruebas

- `RequestCommandServiceImplTest` (30 casos): creación, revisión, permisos y efectos sobre la tarea; `MODIFICATION` (pausa la tarea, comentario obligatorio, no se revisa ni se borra).
- `RequestsContextFacadeImplTest` (3 casos): `hasPendingModification` y `resolvePendingModification`.
- `RequestQueryServiceImplTest` (5 casos): orden, filtros, permiso de líder y grupo inexistente.

## Observaciones

Sin observaciones abiertas: los hallazgos de esta parte están resueltos (ver [AUDITORIA.md](../../../AUDITORIA.md)).

