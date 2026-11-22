# Subscriptions: planes y límite de grupos

Modelo freemium. Cada usuario tiene un **plan** que limita cuántos grupos puede **liderar activos** a la vez (los grupos en los que solo es integrante no cuentan). El pago es **simulado**: no se cobra ni se guarda ningún dato de pago.

| Plan | Precio simulado | Grupos liderados activos |
|---|---|---|
| Gratis (`FREE`) | $0 | 2 |
| Pro (`PRO`) | $9 / mes | 5 |
| Ultra (`ULTRA`) | $29 / mes | sin límite |

Todos empiezan en Gratis: **sin fila en `subscriptions` el usuario está en Gratis**.

## Grupo bloqueado

Si un líder crea un grupo cuando ya llegó al límite de su plan, el grupo **se crea bloqueado** (`groups.locked`): es de **solo lectura para todos** (líder e integrantes).

| Se puede | No se puede (409 `GroupLockedException`) |
|---|---|
| Ver el grupo, sus integrantes, tareas, solicitudes y métricas | Editar el grupo, crear/editar/borrar/cambiar el estado de tareas |
| Salir del grupo, borrarlo (el líder) y cancelar invitaciones | Entregar, pedir modificaciones, revisar o borrar solicitudes |
| El vencimiento automático de tareas sigue corriendo | Invitar, aceptar invitaciones, unirse por código, quitar integrantes (el líder), pedir el informe de IA |

**Liberar un cupo**: el líder borra un grupo activo, o sube de plan. Los grupos bloqueados **no se activan solos**: el líder los activa uno a uno con "Activar grupo" (`POST /groups/{id}/activate`), solo si hay cupo.

**Bajar de plan** con más grupos activos que el nuevo límite: el líder elige exactamente cuántos conservar (`activeGroupIds`); el resto se bloquea. Si la selección no es válida, no cambia nada (ni el plan ni los grupos).

| | |
|---|---|
| **Recursos REST** | `/api/v1/subscriptions/me` · `POST /api/v1/groups/{id}/activate` (lo implementa groups) |
| **Bounded context (backend)** | [`subscriptions`](../../../backend/src/main/java/com/hub/synhub_backend/subscriptions) (el bloqueo vive en [`groups`](../../../backend/src/main/java/com/hub/synhub_backend/groups)) |
| **Módulo (frontend)** | [`src/app/subscriptions`](../../../frontend/src/app/subscriptions) |
| **Rutas de la web** | `/subscription` ("Mi plan") |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Comunicación frontend ↔ backend

| Acción en la web | Frontend | Petición | Backend |
|---|---|---|---|
| Ver mi plan, cupos y catálogo | `Plans` → `SubscriptionsStore.load` | `GET /subscriptions/me` | `SubscriptionController` → `GetSubscriptionOverviewQuery` |
| Cambiar de plan (pago simulado) | `Plans` → `SubscriptionsStore.changePlan` | `PUT /subscriptions/me { plan, activeGroupIds? }` | `SubscriptionController` → `ChangePlanCommand` → `GroupsContextFacade.applyGroupLimit` |
| Activar un grupo bloqueado | `GroupCard`, `GroupDetails` → `GroupsStore.activateGroup` | `POST /groups/{id}/activate` | `GroupController` → `ActivateGroupCommand` |
| Crear un grupo (nace bloqueado si no hay cupo) | `LeaderGroups` → `GroupsStore.addGroup` | `POST /groups` | `GroupCommandServiceImpl` |
