# Groups: backend

Paquete: [`com.hub.synhub_backend.groups`](../../../../backend/src/main/java/com/hub/synhub_backend/groups). Este documento cubre la parte de **grupos y membresías**; las invitaciones, que viven en el mismo paquete, están en [invitations/backend](../../invitations/backend/README.md).

## Endpoints (`GroupController`)

Todos requieren token. El usuario actual se obtiene del token (`@AuthenticationPrincipal`) y se resuelve con `UserQueryService` de IAM.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/groups` | `CreateGroupResource { name, imgUrl?, description? }` | `200 GroupResource` · `400` sin `name` | El creador queda como `GROUP_LEADER`. |
| `GET` | `/api/v1/groups/{groupId}` | — | `200 GroupResource` · `403` · `404` | Solo integrantes del grupo (`GetGroupForMemberQuery`). |
| `GET` | `/api/v1/groups/search` | `?code=` | `200 GroupResource` · `404` | `code` debe cumplir el formato de `GroupCode` o responde 400. |
| `GET` | `/api/v1/groups/{groupId}/members` | — | `200 UserInGroupResource[] { user: PublicUserResource, roleInGroup }` · `403` · `404` | Solo integrantes del grupo. |
| `GET` | `/api/v1/groups/user` | — | `200 GroupResourceReduced[]` (incluye `leader`) | Todos los grupos del usuario. |
| `GET` | `/api/v1/groups/user/role` | `?groupRole=GROUP_LEADER\|GROUP_MEMBER` | `200 GroupResource[]` | Grupos del usuario con ese rol. |
| `PUT` | `/api/v1/groups/{groupId}` | `UpdateGroupResource { name?, imgUrl?, description? }` | `200 GroupResource` · `403` | Solo el líder. Los campos vacíos o ausentes no cambian. |
| `DELETE` | `/api/v1/groups/{groupId}` | — | `204` · `403` · `404` | Solo el líder. Borra primero las tareas del grupo y sus requests (`TasksContextFacade.deleteAllTasksByGroupId`); membresías e invitaciones caen en cascada. Todo es borrado lógico (HUM-03). |
| `POST` | `/api/v1/groups/join` | `JoinGroupResource { code }` | `200 GroupResource` · `400` código mal formado · `404` no existe · `409` ya pertenece | Cualquier usuario con el código entra como `GROUP_MEMBER` (HUM-05). El código se normaliza a mayúsculas. Consume una invitación pendiente al mismo grupo. |
| `DELETE` | `/api/v1/groups/{groupId}/members/{userId}` | — | `204` · `403` · `404` no es integrante · `409` es el líder | El líder quita a un miembro, o un miembro sale (`userId` = él mismo). Borra primero las tareas del integrante en ese grupo y sus requests, después la membresía (HUM-05, borrado lógico). |

`GroupResource = { id, name, imgUrl, description, code, memberCount }`

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Group` | `code: GroupCode` (único), `name`, `description` (TEXT), `imgUrl: ImgUrl`, `userProfiles: GroupProfile[]` y `invitations: Invitation[]` (ambas con `cascade = ALL`). `getLeader()` busca el perfil con rol `GROUP_LEADER`; `getMemberCount()`; `updateInformation(UpdateGroupCommand)`. Borrado lógico (`deleted_at`, HUM-03): borrar un grupo marca también sus tareas, requests, membresías e invitaciones. |
| Entidad | `GroupProfile` | **Membresía**: `user` (IAM), `group`, `roleInGroup`, `tasks` (`@OneToMany(mappedBy = "profile")` hacia `Task` de tasks). `addTask` / `removeTask` mantienen la relación bidireccional; `detachTask` saca una tarea borrada de la colección sin desvincularla. La lista de tareas nunca es `null` (BUG-12). Borrado lógico (`deleted_at`). |
| Value object | `GroupCode` | 9 caracteres `[0-9A-Z]`. `GroupCode.random()` usa `SecureRandom`; `CreateGroupCommand` vuelve a generarlo mientras `existsByCode` sea verdadero. |
| Value object | `ImgUrl` | Normaliza `null`/vacío a `""`. |
| Value object | `GroupRole` | `GROUP_LEADER`, `GROUP_MEMBER`. |

## Capa de aplicación

| Servicio | Mensajes |
|---|---|
| `GroupCommandServiceImpl` | `CreateGroupCommand`, `UpdateGroupCommand`, `DeleteGroupCommand`, `JoinGroupByCodeCommand(code, userId)` y `RemoveMemberUserFromGroupCommand(requesterId, userId, groupId)` (lanza `LeaderCannotBeRemovedException` si se intenta quitar al líder). |
| `GroupQueryServiceImpl` | `GetGroupByIdQuery`, `GetGroupByCodeQuery`, `GetGroupsByUserIdQuery`, `GetGroupsByUserRoleQuery` y `GetGroupMembersByGroupIdQuery`. |

Las reglas de autorización (por ejemplo, "solo el líder") se validan en los command services y lanzan `AccessDeniedException` (403).

## ACL y dependencias con otros contextos

| Dirección | Pieza | Uso |
|---|---|---|
| groups → otros | `GroupsContextFacade.fetchLeaderIdByGroupId` | Lo usa `RequestQueryServiceImpl` (requests) para validar que quien lista las solicitudes sea el líder. |
| groups → otros | `GroupsContextFacade.isMember` / `isLeader` | Control de acceso: `TaskQueryServiceImpl` y `TaskCommandServiceImpl` (tasks), `RequestQueryServiceImpl` (requests) y `MetricsController` (metrics). |
| groups → tasks | `TasksContextFacade.deleteAllTasksByGroupId` y `deleteTasksByUserIdAndGroupId` | `GroupCommandServiceImpl` borra las tareas al borrar el grupo y al quitar a un integrante. |
| groups → iam | `IamContextFacade.fetchUserById` | `GroupCommandServiceImpl` e `InvitationCommandServiceImpl` cargan al líder, al invitado o a quien se une. |
| groups → otros | `GroupsContextFacade.fetchMembership` | `TaskCommandServiceImpl` (tasks): la membresía a la que se asigna una tarea. |
| otros → groups | `GroupQueryService` (directo) | `AiController` y `TaskMetricsQueryServiceImpl` (metrics). |

## Persistencia

`GroupRepository` (`findByCode`, `existsByCode`) y `GroupProfileRepository` (`findByGroup_IdAndUser_Id`, `existsByGroup_IdAndUser_Id`, `findByUser_Id`, `findByGroup_Id`). Las tablas siguen `SnakeCaseWithPluralizedTablePhysicalNamingStrategy` (por ejemplo, `group_profiles`).

## Errores

| Excepción | HTTP |
|---|---|
| `ResourceNotFoundException`, `UserNotInGroupException` | 404 |
| `AccessDeniedException` | 403 |
| `LeaderCannotBeRemovedException`, `UserAlreadyInGroupException` | 409 (extienden `BusinessRuleViolationException`) |

## Observaciones

