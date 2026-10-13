# Groups: grupos de trabajo y membresías

Un **grupo** es el equipo donde se reparten las tareas. Quien lo crea es su **líder** (`GROUP_LEADER`) y los demás entran como **miembros** (`GROUP_MEMBER`) al aceptar una invitación o al ingresar el código del grupo. La membresía (`GroupProfile`) es el ancla del resto del dominio: las tareas se asignan a una membresía, no a un usuario suelto.

| | |
|---|---|
| **Recurso REST** | `/api/v1/groups` |
| **Bounded context (backend)** | [`groups`](../../../backend/src/main/java/com/hub/synhub_backend/groups): también es dueño de [invitations](../invitations/README.md) |
| **Módulo (frontend)** | [`src/app/groups`](../../../frontend/src/app/groups) |
| **Rutas de la web** | `/groups/leader`, `/groups/member`, `/groups?id=…&role=…` |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Qué resuelve

- Crear un grupo: el creador queda como líder y el grupo recibe un **código** único de 9 caracteres (`GroupCode`).
- Listar mis grupos separados por rol (líder / miembro).
- Ver el detalle de un grupo y sus integrantes, y editar nombre, descripción e imagen (solo el líder).
- Unirse a un grupo con su código (HUM-05).
- Quitar a un integrante (líder) o salir del grupo (miembro); sus tareas en el grupo se borran (HUM-05).
- Eliminar un grupo (solo el líder; el backend lo soporta, pero la web todavía no lo usa). Todo borrado es lógico (HUM-03).

## Comunicación frontend ↔ backend

| Acción en la web | Frontend | Petición | Backend |
|---|---|---|---|
| Ver "Mis grupos" como líder / miembro | `LeaderGroups` / `MemberGroups` → `GroupsStore.loadGroups` | `GET /groups/user/role?groupRole=GROUP_LEADER` y `…=GROUP_MEMBER` (en paralelo con `forkJoin`) | `GroupController.getGroupsByUserRole` → `GetGroupsByUserRoleQuery` |
| Crear grupo | `LeaderGroups` (modal) → `GroupsStore.addGroup` | `POST /groups` | `GroupController.createGroup` → `CreateGroupCommand` |
| Ver integrantes | `GroupDetails`, `TaskForm` → `GroupsStore.loadGroupMembers` | `GET /groups/{id}/members` | `GroupController.getGroupMembers` → `GetGroupMembersByGroupIdQuery` |
| Editar grupo | `GroupDetails` → `GroupsStore.updateGroup` | `PUT /groups/{id}` | `GroupController.updateGroup` → `UpdateGroupCommand` |
| Unirse con código | `MemberGroups` → `GroupsStore.joinGroupByCode` | `POST /groups/join` | `GroupController.joinGroup` → `JoinGroupByCodeCommand` |
| Quitar integrante / salir | `GroupDetails` → `GroupsStore.removeMember` / `leaveGroup` | `DELETE /groups/{id}/members/{userId}` | `GroupController.removeMember` → `RemoveMemberUserFromGroupCommand` |

Endpoints que expone el backend y la web no consume: `GET /groups/{id}`, `GET /groups/search?code=`, `GET /groups/user` y `DELETE /groups/{id}`.

## Propiedad compartida

- **Invitations** es un recurso REST propio (`/invitations`), pero su agregado, sus servicios y su controlador viven en el paquete `groups` del backend: aceptar una invitación crea una membresía, que es responsabilidad de groups. En el frontend tiene su propio módulo. Ver [invitations](../invitations/README.md).
- **`GroupProfile` (membresía) contiene las tareas** (`@OneToMany tasks`) y `Task.profile` apunta a ella. El agregado `Task` es de [tasks](../tasks/README.md), pero la relación persistida pasa por esta entidad de groups.
- **`GET /groups/{groupId}/requests`** está bajo la URL de groups, pero lo implementa `GroupRequestController` en el contexto **requests**. Ver [requests](../requests/README.md).
- **`GET /ai/groups/{groupId}/report`** consulta un grupo, pero le pertenece a [ai](../ai/README.md).
- **Otros contextos leen repositorios de groups directamente**: `TaskCommandServiceImpl` (tasks) usa `GroupRepository` y `GroupProfileRepository`, y `UserQueryServiceImpl` (iam) usa `GroupProfileRepository`. La ACL formal de groups (`GroupsContextFacade`) solo la usa requests.
- **Frontend**: `GroupDetails` es la vista con más acoplamiento: combina `GroupsStore`, `TasksStore` (tablero del grupo), `InvitationsStore` (invitar y cancelar) e `IamStore`.

Más detalle en [Propiedad entre features](../../architecture/cross-feature-ownership.md).
