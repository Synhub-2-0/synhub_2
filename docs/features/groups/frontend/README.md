# Groups: frontend

Carpeta: [`frontend/src/app/groups`](../../../../frontend/src/app/groups)

## Rutas y vistas

`groups.routes.ts`, cargado bajo `/groups` con `iamGuard`.

| Ruta | Vista | Stores | Qué hace |
|---|---|---|---|
| `/groups/leader` | `LeaderGroups` | `GroupsStore` | Lista los grupos donde soy líder. `?create=true` abre el modal para crear un grupo (`addGroup`). |
| `/groups/member` | `MemberGroups` | `GroupsStore`, `InvitationsStore` | Lista los grupos donde soy miembro. El panel "Unirse a un grupo" acepta un **código** o muestra **mis invitaciones pendientes** (aceptar / rechazar). |
| `/groups/:id` | `GroupDetails` | `GroupsStore`, `TasksStore`, `InvitationsStore`, `IamStore`, `MetricsStore` | Detalle del grupo (el rol sale de `leaderGroups`, no de la URL; secciones con el mismo diseño de tarjeta y un botón que lleva a Gestión de tareas): integrantes, edición (líder), quitar integrantes con confirmación (líder) o salir del grupo (miembro), tablero de tareas, distribución de carga con donut y desglose de tareas por integrante para el líder, invitaciones enviadas e invitación por nombre de usuario. |

Componentes: `GroupList` → `GroupCard` (navega al detalle o a las tareas del grupo con `queryParams`), `NoGroupLeader` y `NoGroupMember` (estados vacíos). `GroupDetails` reutiliza `TaskBoard` y `TaskList` de [tasks](../../tasks/frontend/README.md) y los componentes de métricas de [metrics](../../metrics/frontend/README.md).

## Estado: `GroupsStore` (`application/groups.store.ts`)

| Signal | Contenido |
|---|---|
| `groups` | Unión de los grupos de líder y de miembro. |
| `leaderGroups`, `memberGroups` | Grupos por rol. |
| `membersByGroup` | `Record<groupId, GroupUser[]>`. |
| `loading`, `membersLoading`, `joining`, `error`, `groupCount` | Estado de la UI. |

| Método | Petición |
|---|---|
| `loadGroups()` | `forkJoin` de `GET /groups/user/role?groupRole=GROUP_LEADER` y `…=GROUP_MEMBER` |
| `loadGroupMembers(groupId)` | `GET /groups/{id}/members` |
| `addGroup(command, router)` | `POST /groups` |
| `updateGroup(group, router)` | `PUT /groups/{id}`: envía la entidad completa con `BaseApiEndpoint.update`; el backend solo toma `name`, `imgUrl` y `description`. |
| `joinGroupByCode(code, onSuccess?)` | `POST /groups/join`. Valida el formato (`[0-9A-Z]{9}`) antes de llamar y traduce 404/409/400 a mensajes en español. |
| `removeMember(groupId, userId, onSuccess?)` | `DELETE /groups/{id}/members/{userId}`; saca al integrante de `membersByGroup`. |
| `leaveGroup(groupId, router)` | `DELETE /groups/{id}/members/{yo}` y navega a `/groups/member`. |
| `getGroupById(id)` | No hace petición: devuelve un `computed` sobre `groups`. |

Los errores se muestran con `ToastStore` (shared).

## Infraestructura

| Archivo | Rol |
|---|---|
| `groups.api.ts` (`GroupsApi`) | Fachada inyectable: `getLeaderGroups`, `getMemberGroups`, `getGroupMembers`, `createGroup`, `updateGroup`, `joinGroupByCode` y `removeMember`. |
| `groups.api-endpoint.ts` | Extiende `BaseApiEndpoint` y agrega `createGroup`, `getGroupMembers`, `getGroupsByUserGroupRole`, `joinByCode` y `removeMember`. |
| `groups.assembler.ts` | `GroupResource` ↔ `Group`; `GroupUsersResource` → `GroupUser`, usando `ProfilesAssembler` de IAM. |
| `groups.response.ts`, `create-group.request.ts` | Tipos de la API. |

## Modelo

| Clase | Campos |
|---|---|
| `Group` | `id`, `name`, `imgUrl`, `description`, `code`, `memberCount` |
| `GroupUser` | `user: Profile` (IAM), `roleInGroup` |
| `CreateGroupCommand` | `name`, `description`, `imgUrl` |

## Dependencias con otras features

- `GroupsAssembler` → `ProfilesAssembler` ([iam](../../iam/frontend/README.md)).
- `GroupDetails` → `TasksStore` ([tasks](../../tasks/frontend/README.md)) e `InvitationsStore` ([invitations](../../invitations/frontend/README.md)).
- La distribución de carga en `GroupDetails` → `MetricsStore` y componentes de [metrics](../../metrics/frontend/README.md); las tareas y los integrantes los proveen `TasksStore` y `GroupsStore`.
- Al borrar una tarea o quitar un integrante desde `GroupDetails`, se recargan las métricas y tareas para mantener actualizado el desglose.
- `GroupsStore` lo consumen `Home`, `LeaderTasks`, `MemberTasks`, `TaskForm` y `Validations`, que necesitan saber en qué grupos lidera o participa el usuario.

## Pruebas

`group-card`, `group-list`, `no-group-leader`, `no-group-member`, `group-details`, `leader-groups` y `member-groups` (`*.spec.ts`).

## Grupos bloqueados

`Group.locked` viene del backend. `GroupCard` muestra la insignia "Bloqueado" y, al líder, "Activar grupo" (deshabilitado sin cupo; carga `SubscriptionsStore` si hace falta). `GroupDetails` muestra un aviso de solo lectura, con "Activar grupo" y "Ver mi plan" para el líder, y oculta la edición, las invitaciones y quitar integrantes (`canManage = isLeaderView && !isLocked`). `GroupsStore.activateGroup` reemplaza el grupo en las listas; `addGroup` avisa si el grupo nació bloqueado. El líder puede **eliminar el grupo** desde `GroupDetails` ("Eliminar grupo", con confirmación; también si está bloqueado): `GroupsStore.deleteGroup` hace `DELETE /groups/{id}`, lo quita de las listas, refresca los cupos del plan y vuelve a Grupos Liderados. Es la forma de liberar un cupo sin cambiar de plan.
