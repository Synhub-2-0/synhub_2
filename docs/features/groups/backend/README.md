# Groups: backend

Paquete: [`com.hub.synhub_backend.groups`](../../../../backend/src/main/java/com/hub/synhub_backend/groups). Este documento cubre la parte de **grupos y membresías**; las invitaciones, que viven en el mismo paquete, están en [invitations/backend](../../invitations/backend/README.md).

## Endpoints (`GroupController`)

Todos requieren token. El usuario actual se obtiene del token (`@AuthenticationPrincipal`) y se resuelve con `UserQueryService` de IAM.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/groups` | `CreateGroupResource { name, imgUrl?, description? }` | `200 GroupResource` · `400` sin `name` | El creador queda como `GROUP_LEADER`. |
| `GET` | `/api/v1/groups/{groupId}` | — | `200 GroupResource` · `403` · `404` | Solo integrantes del grupo (`GetGroupForMemberQuery`). |
| `GET` | `/api/v1/groups/search` | `?code=` | `200 GroupResource` · `404` | `code` debe cumplir el formato de `GroupCode` o responde 400. |
| `GET` | `/api/v1/groups/{groupId}/members` | — | `200 UserInGroupResource[] { user: UserResource, roleInGroup }` · `403` · `404` | Solo integrantes del grupo. |
| `GET` | `/api/v1/groups/user` | — | `200 GroupResourceReduced[]` (incluye `leader`) | Todos los grupos del usuario. |
| `GET` | `/api/v1/groups/user/role` | `?groupRole=GROUP_LEADER\|GROUP_MEMBER` | `200 GroupResource[]` | Grupos del usuario con ese rol. |
| `PUT` | `/api/v1/groups/{groupId}` | `UpdateGroupResource { name?, imgUrl?, description? }` | `200 GroupResource` · `403` | Solo el líder. Los campos vacíos o ausentes no cambian. |
| `DELETE` | `/api/v1/groups/{groupId}` | — | `204` · `403` · `404` | Solo el líder. |

`GroupResource = { id, name, imgUrl, description, code, memberCount }`

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Group` | `code: GroupCode` (único), `name`, `description` (TEXT), `imgUrl: ImgUrl`, `userProfiles: GroupProfile[]` y `invitations: Invitation[]` (ambas con `cascade = ALL`). `getLeader()` busca el perfil con rol `GROUP_LEADER`; `getMemberCount()`; `updateInformation(UpdateGroupCommand)`. |
| Entidad | `GroupProfile` | **Membresía**: `user` (IAM), `group`, `roleInGroup`, `tasks` (`@OneToMany(mappedBy = "profile")` hacia `Task` de tasks). `addTask` / `removeTask` mantienen la relación bidireccional. |
| Value object | `GroupCode` | 9 caracteres `[0-9A-Z]`. `GroupCode.random()` usa `SecureRandom`; `CreateGroupCommand` vuelve a generarlo mientras `existsByCode` sea verdadero. |
| Value object | `ImgUrl` | Normaliza `null`/vacío a `""`. |
| Value object | `GroupRole` | `GROUP_LEADER`, `GROUP_MEMBER`. |

## Capa de aplicación

| Servicio | Mensajes |
|---|---|
| `GroupCommandServiceImpl` | `CreateGroupCommand`, `UpdateGroupCommand`, `DeleteGroupCommand` y `RemoveMemberUserFromGroupCommand` (este último no tiene endpoint; lanza `LeaderCannotBeRemovedException` si se intenta quitar al líder). |
| `GroupQueryServiceImpl` | `GetGroupByIdQuery`, `GetGroupByCodeQuery`, `GetGroupsByUserIdQuery`, `GetGroupsByUserRoleQuery`, `GetGroupMembersByGroupIdQuery`, `GetGroupsByLeaderIdQuery` (sin uso). |

Las reglas de autorización (por ejemplo, "solo el líder") se validan en los command services y lanzan `AccessDeniedException` (403).

## ACL y dependencias con otros contextos

| Dirección | Pieza | Uso |
|---|---|---|
| groups → otros | `GroupsContextFacade.fetchLeaderIdByGroupId` | Lo usa `RequestQueryServiceImpl` (requests) para validar que quien lista las solicitudes sea el líder. |
| groups → otros | `GroupsContextFacade.isMember` / `isLeader` | Control de acceso de las lecturas: `TaskQueryServiceImpl` (tasks) y `RequestQueryServiceImpl` (requests). |
| groups → tasks | `application/external/ExternalTasksService` → `TasksContextFacade.deleteTasksByUserIdAndGroupId` | Está preparado para borrar las tareas de un miembro al quitarlo del grupo, pero todavía nadie lo llama. |
| groups → iam | `UserRepository` (directo) | `GroupCommandServiceImpl` busca al líder y lo guarda. |
| otros → groups | `GroupRepository`, `GroupProfileRepository` (directo) | `TaskCommandServiceImpl` (tasks) y `UserQueryServiceImpl` (iam). |
| otros → groups | `GroupQueryService` (directo) | `AiController`, `TaskMetricsQueryServiceImpl` y `GroupsClient` (metrics). |

## Persistencia

`GroupRepository` (`findByCode`, `existsByCode`, `findByLeaderId`) y `GroupProfileRepository` (`findByGroup_IdAndUser_Id`, `existsByGroup_IdAndUser_Id`, `findByUser_Id`, `findByGroup_Id`). Las tablas siguen `SnakeCaseWithPluralizedTablePhysicalNamingStrategy` (por ejemplo, `group_profiles`).

## Errores

| Excepción | HTTP |
|---|---|
| `ResourceNotFoundException`, `UserNotInGroupException` | 404 |
| `AccessDeniedException` | 403 |
| `LeaderCannotBeRemovedException` | 500 (extiende `RuntimeException`, no `BusinessRuleViolationException`) |

## Observaciones

- `GroupRepository.findByLeaderId` está derivado de una propiedad `leader` que `Group` no tiene como campo: el líder se calcula desde `userProfiles`. Solo lo usa `GetGroupsByLeaderIdQuery`, que tampoco se invoca. Revisarlo antes de usarlo.
- `LeaderCannotBeRemovedException` debería extender `BusinessRuleViolationException` para responder 409.
