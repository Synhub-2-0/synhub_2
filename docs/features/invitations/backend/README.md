# Invitations: backend

> El código de esta feature vive en el bounded context **groups**: [`com.hub.synhub_backend.groups`](../../../../backend/src/main/java/com/hub/synhub_backend/groups). No existe un paquete `invitations`.

| Capa | Clases |
|---|---|
| `interfaces/rest` | `InvitationController` |
| `interfaces/rest/resources` | `CreateInvitationResource`, `InvitationResource` |
| `interfaces/rest/transform` | `CreateInvitationCommandFromResourceAssembler`, `InvitationResourceFromEntityAssembler` |
| `domain/model/aggregates` | `Invitation` |
| `domain/model/commands` | `CreateInvitationCommand`, `AcceptInvitationCommand`, `CancelInvitationCommand` |
| `domain/model/queries` | `GetInvitationsByUserIdQuery`, `GetInvitationsByGroupIdQuery` |
| `domain/services` | `InvitationCommandService`, `InvitationQueryService` |
| `application/internal` | `InvitationCommandServiceImpl`, `InvitationQueryServiceImpl` |
| `infrastructure/persistence/jpa/repositories` | `InvitationRepository` |
| `domain/exceptions` | `UserAlreadyHasInvitationForGroupException`, `UserAlreadyInGroupException` |

## Endpoints (`InvitationController`)

Todos requieren token.

| Método | Ruta | Cuerpo / parámetros | Respuesta | Regla |
|---|---|---|---|---|
| `POST` | `/api/v1/invitations` | `CreateInvitationResource { userId, groupId }` | `200 InvitationResource` | Solo el líder del grupo (403). 409 si el usuario ya tiene una invitación a ese grupo o ya es miembro. 404 si no existe el usuario o el grupo. |
| `POST` | `/api/v1/invitations/accept` | `?invitationId=` | `200` (sin cuerpo) | Solo el invitado (403). 409 si ya es miembro. |
| `DELETE` | `/api/v1/invitations/{invitationId}` | — | `204` | El invitado (rechaza) o el líder del grupo (cancela); cualquier otro usuario recibe 403. |
| `GET` | `/api/v1/invitations/user` | — | `200 InvitationResource[]` | Invitaciones recibidas por el usuario del token. |
| `GET` | `/api/v1/invitations/group` | `?groupId=` | `200 InvitationResource[]` · `403` · `404` | Invitaciones pendientes de un grupo. Solo el líder. |

`InvitationResource = { id, group: GroupResourceReduced, user: PublicUserResource }`

## Modelo de dominio

`Invitation` (agregado, extiende `AuditableAbstractAggregateRoot`): `user` (`@ManyToOne` hacia el `User` de IAM, columna `member_id`) y `group` (`@ManyToOne` hacia `Group`). No tiene estado: **existir** (sin `deleted_at`) equivale a "pendiente". Aceptarla o rechazarla la borra de forma lógica (HUM-03): deja de verse, pero queda como historial.

## Reglas (`InvitationCommandServiceImpl`)

1. **Crear**: carga el usuario (`IamContextFacade.fetchUserById` de IAM) y el grupo; exige que el solicitante sea `group.getLeader()`; rechaza duplicados (`existsByUser_IdAndGroup_Id`) y miembros existentes (`GroupProfileRepository.existsByGroup_IdAndUser_Id`).
2. **Aceptar**: exige que el solicitante sea el invitado y crea `new GroupProfile(user, group, GROUP_MEMBER)`. **Aquí se crea la membresía**, por eso la feature le pertenece a groups.
3. **Cancelar**: exige que el solicitante sea el invitado o el líder.

## Observaciones

- `InvitationRepository.findByUser_IdAndGroup_Id` no se usa.
