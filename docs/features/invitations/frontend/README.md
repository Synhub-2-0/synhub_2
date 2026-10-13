# Invitations: frontend

Carpeta: [`frontend/src/app/invitations`](../../../../frontend/src/app/invitations). Tiene `application`, `domain` e `infrastructure`, pero **no tiene `presentation`**: la UI la ponen las vistas de [groups](../../groups/frontend/README.md).

## Dónde se usa

| Vista (de groups) | Qué muestra | Métodos del store |
|---|---|---|
| `MemberGroups` (`/groups/member`) | Invitaciones recibidas, con botones para aceptar y rechazar. Al aceptar se recargan los grupos. | `loadInvitations`, `acceptInvitation`, `declineInvitation`, `invitations` |
| `GroupDetails` (`/groups?id=…`) | Formulario "invitar por nombre de usuario" y lista de invitaciones enviadas (cancelables). | `inviteByUsername`, `loadGroupInvitations`, `declineInvitation`, `clearError` |

## Estado: `InvitationsStore` (`application/invitations.store.ts`)

| Signal | Contenido |
|---|---|
| `invitations` | Invitaciones recibidas por el usuario. |
| `groupInvitations` | Invitaciones enviadas del grupo abierto. |
| `loading`, `inviting`, `error`, `invitationCount` | Estado de la UI. |

| Método | Peticiones | Notas |
|---|---|---|
| `inviteByUsername(username, groupId, onSuccess?)` | `GET /users/username?username=` → `POST /invitations` | Usa `IamApi.getProfileByUsername` (IAM) para obtener el `userId`. Si el usuario no existe, muestra un error amigable. |
| `loadInvitations()` | `GET /invitations/user` | — |
| `loadGroupInvitations(groupId)` | `GET /invitations/group?groupId=` | — |
| `acceptInvitation(id, onSuccess?)` | `POST /invitations/accept?invitationId=` | Usa `retry(2)`. |
| `declineInvitation(id)` | `DELETE /invitations/{id}` | Usa `retry(2)`. Sirve tanto para rechazar como para cancelar. |

## Infraestructura

| Archivo | Rol |
|---|---|
| `invitations.api.ts` (`InvitationsApi`) | Fachada: `createInvitation`, `acceptInvitation`, `declineInvitation`, `getInvitationsByGroupId`, `getInvitationsByAuthenticatedUser`. |
| `invitations.api-endpoint.ts` | Extiende `BaseApiEndpoint`. `createInvitation` traduce los códigos HTTP a mensajes en español (403 → "Solo el líder…", 409 → "ya es miembro" / "ya tiene una invitación pendiente"). Unirse por iniciativa propia se hace con el código del grupo (`GroupsStore.joinGroupByCode`, ver [groups](../../groups/frontend/README.md)). |
| `invitations.assembler.ts` | `InvitationResource` → `Invitation`, usando `GroupsAssembler` (groups) y `ProfilesAssembler` (iam). |

## Modelo

| Clase | Campos |
|---|---|
| `Invitation` | `id`, `group: Group` (groups), `profile: Profile` (iam). El backend lo llama `user`; el assembler lo renombra a `profile`. |
| `CreateInvitationCommand` | `groupId`, `userId` |

## Pruebas

No hay `*.spec.ts` propios; el comportamiento se cubre de forma indirecta en `member-groups.spec.ts` y `group-details.spec.ts`.
