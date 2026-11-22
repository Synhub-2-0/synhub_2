# Subscriptions: backend

Paquete: [`com.hub.synhub_backend.subscriptions`](../../../../backend/src/main/java/com/hub/synhub_backend/subscriptions)

## Endpoints

Todos requieren token; el usuario sale siempre de la sesión.

### `SubscriptionController`: `/api/v1/subscriptions`

| Método | Ruta | Cuerpo | Respuesta | Regla |
|---|---|---|---|---|
| `GET` | `/subscriptions/me` | — | `200 SubscriptionResource` | Plan actual, grupos liderados activos y bloqueados, cupos libres y catálogo de planes. |
| `PUT` | `/subscriptions/me` | `ChangePlanResource { plan (obligatorio), activeGroupIds? }` | `200 SubscriptionResource` | Pago simulado. Plan desconocido o vacío → 400. Bajar a un límite menor que los grupos activos exige `activeGroupIds` con exactamente ese número de grupos activos que el usuario lidera (409 `InvalidActiveGroupSelectionException`). |

`SubscriptionResource = { plan, planName, monthlyPrice, maxLedGroups (null = sin límite), activeLedGroups, lockedLedGroups, availableSlots (null = sin límite), plans: [{ plan, name, monthlyPrice, maxLedGroups }] }`

El grupo activado lo implementa **groups**: `POST /groups/{groupId}/activate` (solo su líder; 409 si no hay cupo `NoFreeGroupSlotException` o ya está activo `GroupAlreadyActiveException`). Ver [groups](../../groups/backend/README.md).

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `Subscription` (tabla `subscriptions`) | `userId` (único; solo el id: iam es otro contexto) y `plan`. Borrado lógico. |
| Value object | `SubscriptionPlan` | `FREE` (2), `PRO` (5), `ULTRA` (`UNLIMITED` = `Integer.MAX_VALUE`); nombre y precio simulado. `fromString` no distingue mayúsculas. |
| Value object | `SubscriptionOverview` | `plan`, `activeLedGroups`, `lockedLedGroups`; calcula `maxLedGroups` y `availableSlots` (null si no hay límite). |

## Reglas

- `SubscriptionCommandServiceImpl.handle(ChangePlanCommand)`: llama a `GroupsContextFacade.applyGroupLimit(userId, límite, activeGroupIds)` **antes** de guardar el plan, en una sola transacción: si la selección es inválida no cambia nada.
- Subir de plan **no** activa grupos bloqueados (los activa el líder).
- `SubscriptionQueryServiceImpl` cuenta los grupos liderados con `GroupsContextFacade.fetchLedGroups`.

## Dependencias

| Hacia | Mediante | Para |
|---|---|---|
| groups | `GroupsContextFacade` (ACL) | Listar los grupos que lidera el usuario y aplicar el límite al cambiar de plan. |
| iam | `UserQueryService` (en el controlador) | Resolver el usuario de la sesión. |

Subscriptions **ofrece** `SubscriptionsContextFacade.maxLedGroups(userId)` a groups (al crear y activar grupos). Su implementación usa solo `SubscriptionRepository` para no formar un ciclo de beans (groups ↔ subscriptions).

## Persistencia

Migración `V4__subscriptions_and_group_lock.sql`: tabla `subscriptions` (`user_id` único, `plan` con `check`) y `groups.locked`. Los grupos que ya existían respetan el plan Gratis: los que exceden 2 por líder (por `id`) quedan bloqueados.

## Errores

| Excepción | HTTP |
|---|---|
| `IllegalArgumentException` (plan desconocido), validación `@NotBlank` | 400 |
| `InvalidActiveGroupSelectionException` | 409 |

## Pruebas

- `SubscriptionCommandServiceImplTest` (5), `SubscriptionsContextFacadeImplTest` (3), `SubscriptionQueryServiceImplTest` (3).
- `api/SubscriptionApiTests` (10) y `api/GroupLockApiTests` (7): el flujo completo por HTTP.
