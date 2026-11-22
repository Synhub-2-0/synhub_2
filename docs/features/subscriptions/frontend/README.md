# Subscriptions: frontend

Módulo: [`src/app/subscriptions`](../../../../frontend/src/app/subscriptions)

## Rutas

| Ruta | Vista | Stores | Descripción |
|---|---|---|---|
| `/subscription` | `Plans` (`presentation/views/plans`) | `SubscriptionsStore`, `GroupsStore` | "Mi plan": plan actual y uso, comparación de planes y cambio con pago simulado. Enlace "Mi plan" en el menú lateral. |

## Estado: `SubscriptionsStore` (`application/subscriptions.store.ts`)

| Signal | Contenido |
|---|---|
| `subscription` | `Subscription` (plan, grupos activos/bloqueados, cupos, catálogo) o `null`. |
| `loading`, `changing`, `error` | Estado de la UI. Se vacía al cerrar sesión. |

| Método | Petición |
|---|---|
| `load()` | `GET /subscriptions/me` |
| `changePlan(command, onSuccess?)` | `PUT /subscriptions/me` con `{ plan, activeGroupIds? }` (sin datos de pago); avisa con un toast. |

## Vista `Plans`

- Tarjetas de los tres planes con su precio simulado y límite; el actual va marcado.
- Al elegir uno abre la **confirmación** ("Pago simulado: no se cobra nada ni se guarda ningún dato de pago").
- Si el límite del plan nuevo es menor que los grupos activos, pide **elegir exactamente cuántos siguen activos** (sugiere los más antiguos; los bloqueados no se ofrecen). Confirmar queda deshabilitado hasta que la selección sea exacta.
- Al terminar recarga los grupos para refrescar los bloqueados.
- Avisa cuántos grupos están bloqueados y cómo activarlos.

## Infraestructura

| Archivo | Rol |
|---|---|
| `subscriptions.api.ts` / `subscriptions.api-endpoint.ts` | `getMySubscription` y `changePlan`; traducen 400, 409 y 500 a mensajes en español. |
| `subscriptions.assembler.ts` | `SubscriptionResource` → `Subscription` (con su catálogo de `Plan`); comando → cuerpo. |

## Bloqueo en otras vistas

`Group.locked` y `TaskGroup.locked` llegan con el grupo. Con el grupo bloqueado: la tarjeta (`GroupCard`) muestra "Bloqueado" y, al líder, "Activar grupo" (deshabilitado sin cupo); `GroupDetails` muestra un aviso y oculta edición, invitaciones y quitar integrantes; `LeaderTasks` oculta "Crear Tarea"; las tarjetas, el tablero, `TaskMemberActions` y `Validations` no ofrecen acciones. Ver [groups](../../groups/frontend/README.md) y [tasks](../../tasks/frontend/README.md).

## Pruebas

`subscriptions.api.spec.ts`, `subscriptions.assembler.spec.ts`, `subscriptions.store.spec.ts` y `plans.spec.ts`.
