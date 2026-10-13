# IAM: backend

Paquete: [`com.hub.synhub_backend.iam`](../../../../backend/src/main/java/com/hub/synhub_backend/iam)

## Endpoints

| Método | Ruta | Acceso | Cuerpo / parámetros | Respuesta |
|---|---|---|---|---|
| `POST` | `/api/v1/authentication/sign-in` | Público | `SignInResource { username, password }` | `200 AuthenticatedUserResource { id, username, token, role[] }` · `401` credenciales inválidas |
| `POST` | `/api/v1/authentication/sign-up` | Público | `SignUpResource { username, name, surname, imgUrl?, email, password }` (no acepta roles: siempre `ROLE_USER`; `email` válido, `password` de 8 a 72 caracteres; sin `imgUrl` se usa `/default-avatar.jpg`) | `201 UserResource` · `409` usuario o email repetido |
| `GET` | `/api/v1/users` | Solo `ROLE_ADMIN` | — | `200 UserResource[]` · `403` para el resto |
| `GET` | `/api/v1/users/me` | Autenticado | — | `200 UserResource` del usuario de la sesión (único que incluye los roles) |
| `GET` | `/api/v1/users/{userId}` | Autenticado | — | `200 PublicUserResource` · `404` |
| `GET` | `/api/v1/users/username` | Autenticado | `?username=` | `200 PublicUserResource` · `404` |
| `GET` | `/api/v1/roles` | Autenticado | — | `200 RoleResource[] { id, name }` |

`UserResource = { id, username, name, surname, imgUrl, email, accountRoles[] }`

`PublicUserResource = { id, username, name, surname, imgUrl, email }`: lo que un usuario puede ver de otro (nombre, correo y foto; decisión HUM-06). Se usa en `/users/{id}`, `/users/username` y en todos los usuarios anidados (`TaskResource.assignedTo`, `GroupResourceReduced.leader`, `InvitationResource.user`, miembros de un grupo).

## Modelo de dominio

| Tipo | Clase | Notas |
|---|---|---|
| Agregado | `User` | `username` (único, ≤50), `name`, `surname`, `imgUrl` (≤1024), `email` (único), `password` (hash BCrypt), `accountRoles` (`@ManyToMany` EAGER con `Role`, tabla `user_roles`), `groupProfiles` (relación inversa hacia groups). |
| Entidad | `Role` | `name: AccountRole`. `Role.validateRoleSet` asigna `ROLE_USER` si la lista llega vacía. |
| Value object | `AccountRole` | `ROLE_USER`, `ROLE_ADMIN`. Son roles **de cuenta**; los roles dentro de un grupo (`GROUP_LEADER`/`GROUP_MEMBER`) son de [groups](../../groups/backend/README.md). |

## Capa de aplicación (CQRS)

| Servicio | Mensajes que maneja |
|---|---|
| `UserCommandServiceImpl` | `SignInCommand` → valida con `HashingService`, genera el token con `TokenService` y devuelve `(User, token)`.<br>`SignUpCommand` → valida unicidad, resuelve los roles en `RoleRepository` y guarda el usuario. Desde el registro público el comando siempre trae `ROLE_USER` (`SignUpCommandFromResourceAssembler`). |
| `RoleCommandServiceImpl` | `SeedRolesCommand` → crea los roles de `AccountRole` que falten. |
| `UserQueryServiceImpl` | `GetAllUsersQuery`, `GetUserByIdQuery`, `GetUserByUsernameQuery`, `GetUsersByGroupIdQuery` (esta última lee `GroupProfileRepository` de groups). |
| `RoleQueryServiceImpl` | `GetAllRolesQuery`, `GetRoleByNameQuery`. |
| `ApplicationReadyEventHandler` | Al recibir `ApplicationReadyEvent`, ejecuta `SeedRolesCommand`: los roles siempre existen al arrancar. |

**Mensajes definidos sin uso:** `UpdateUserInfoCommand`, `GetUserByLeaderId` y `GetUserByMemberId` no tienen handler. `CreateUserLeaderCommand` solo tiene un assembler.

## ACL hacia otros contextos

`IamContextFacade` (interfaz en `interfaces/acl`, implementación en `application/internal/acl`) expone `createUser`, `fetchUserIdByUsername`, `fetchUsernameByUserId`, `fetchUserById` y `fetchUsersByGroupId`. `shared/application/external/outboundedservices/ExternalIamService` lo envuelve, pero ninguna clase lo consume todavía.

## Infraestructura

| Pieza | Clase | Detalle |
|---|---|---|
| Persistencia | `UserRepository`, `RoleRepository` | Spring Data JPA. `findByUsername`, `existsByUsername`, `existsByEmail`, `findByName`, `existsByName`. |
| Hashing | `BCryptHashingService` | Implementa `HashingService` (puerto de aplicación) y `PasswordEncoder`. |
| Tokens | `TokenServiceImpl` | JJWT. Secreto en `authorization.jwt.secret` (variable `JWT_SECRET`, obligatoria y de al menos 32 bytes: `validateSecret()` lo comprueba al arrancar) y expiración en `authorization.jwt.expiration.days` (7). |
| Seguridad | `WebSecurityConfiguration` | Stateless, CSRF deshabilitado, CORS `*`. Rutas públicas: `/api/v1/authentication/**`, `/actuator/health` y Swagger (`/v3/api-docs/**`, `/swagger-ui/**`). El resto requiere token. |
| Filtro | `BearerAuthorizationRequestFilter` | Lee `Authorization: Bearer …`, valida el token y carga el `UserDetails` (`UserDetailsServiceImpl`) en el `SecurityContext`. |
| 401 | `UnauthorizedRequestHandlerEntryPoint` | Respuesta cuando falta el token o no es válido. |

## Errores

| Excepción | HTTP |
|---|---|
| `UsernameAlreadyExistsException`, `EmailAlreadyExistsException` | 409 (`BusinessRuleViolationException`) |
| `BadCredentialsException` (sign-in) | 401 |

## Observaciones

- `GET /users` devuelve todos los usuarios con su email a cualquier usuario autenticado. El frontend no lo usa.
- `GET /roles` no lo usa el frontend.
