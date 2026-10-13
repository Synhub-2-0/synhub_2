# IAM: frontend

Carpeta: [`frontend/src/app/iam`](../../../../frontend/src/app/iam)

## Rutas y vistas

| Ruta | Componente | Qué hace |
|---|---|---|
| `/auth/sign-in` | `SignInPage` (`presentation/views/sign-in-page`) | Formulario de login. Llama a `IamStore.signIn` y, si sale bien, navega a `/home`. |
| `/auth/sign-up` | `SignUpPage` (`presentation/views/sign-up-page`) | Formulario de registro (contraseña de 8 a 72 caracteres, igual que el backend; contraseñas coincidentes; email válido). No envía roles: el backend asigna `ROLE_USER`. Si sale bien, inicia sesión automáticamente y navega a `/home`. |
| `''` | — | Redirige a `/auth/sign-in` (definido en `app.routes.ts`). |

Las rutas de `iam.routes.ts` se cargan de forma diferida (`loadChildren`) y **no** tienen guard.

## Estado: `IamStore` (`application/iam.store.ts`)

Store singleton (`providedIn: 'root'`) basado en signals. Funciona como "contexto global" de la sesión.

| Signal | Origen |
|---|---|
| `isSignedIn` | `hasActiveSession()` al iniciar (hay `userId` y `sessionExpiresAt` no venció); `true` tras el login. |
| `currentUserId` | `localStorage.userId` si la sesión está vigente. |
| `currentUsername`, `currentProfile` | Respuesta del login, o de `/users/me` al restaurar la sesión. |

**La web nunca ve el JWT** (decisión HUM-04): viaja en la cookie `HttpOnly` `SYNHUB_SESSION`, válida por 1 día, que el navegador envía solo. `infrastructure/session.ts` guarda en `localStorage` únicamente `userId` y `sessionExpiresAt`, para que el guard y los stores sepan si hay sesión sin llamar a la API.

| Método | Peticiones | Efecto |
|---|---|---|
| `signIn(command, router)` | `POST /authentication/sign-in` → `GET /users/{id}` | El backend abre la sesión con la cookie; el store guarda `userId` y `expiresAt`, carga el perfil y navega a `/home`. |
| `signUp(command, router)` | `POST /authentication/sign-up` → `signIn` con las mismas credenciales | Si sale bien, la cuenta queda con sesión iniciada y navega a `/home`; si falla, muestra un toast y vuelve a `/auth/sign-up`. |
| `signOut(router)` | `POST /authentication/sign-out` | El backend borra la cookie; el store limpia la sesión local (aunque la petición falle) y navega a `/auth/sign-in`. |
| `endSession()` | — | Limpia la sesión local sin llamar a la API. Lo usan el interceptor (401) y el guard (sesión vencida). |
| `restoreSession()` | `GET /users/me` → `GET /users/{id}` | Se ejecuta en `provideAppInitializer` (`app.config.ts`), antes del primer render, solo si hay una sesión vigente. |
| `loadUsers()` | — | Incompleto: solo activa `loadingUsers`. |

## Infraestructura

| Archivo | Rol |
|---|---|
| `iam.api.ts` (`IamApi`) | Fachada que compone cuatro endpoints: sign-in, sign-up, users y profiles. |
| `sign-in/*`, `sign-up/*` | `*ApiEndpoint` (extienden `ErrorHandlingEnabledBaseType`), `*Assembler`, `*Request`, `*Response`. |
| `users/*` | `UsersApiEndpoint` (`BaseApiEndpoint`): `getByAuthentication()` → `/users/me`, `getByUsername()`. |
| `profiles/*` | `ProfilesApiEndpoint`: construye un `Profile` a partir de `/users/{id}` o `/users/username`. **No existe un recurso `/profiles` en el backend**: el perfil se arma con el `PublicUserResource`. |
| `session.ts` | Pista local de la sesión: `saveSession`, `clearStoredSession`, `storedUserId` y `hasActiveSession`. |
| `iam.interceptor.ts` | `HttpInterceptorFn`: las peticiones a la API salen con `withCredentials` (la cookie de sesión). Un 401 fuera de `/authentication/` llama a `IamStore.endSession()` y redirige a `/auth/sign-in?expired=1`, donde el login muestra "Tu sesión expiró". Se registra en `app.config.ts`. |
| `iam.guard.ts` | `CanActivateFn`: deja pasar solo si `isSignedIn()` y la sesión no venció; si venció, la cierra. En otro caso redirige a `/auth/sign-in`. Protege `home`, `groups`, `tasks`, `validations` y `recorrido`. |

## Modelo

| Clase | Campos |
|---|---|
| `User` | `id`, `username`, `accountRoles` |
| `Profile` | `id`, `name`, `surname`, `email`, `imgUrl`, `userId`, `username`. `id` y `userId` son el id del usuario: el assembler toma `userId` de `id`, porque `PublicUserResource` no envía otro (BUG-15). |
| `SignInCommand`, `SignUpCommand` | Datos de los formularios. |

## Consumidores fuera de IAM

- `Layout` (shared): muestra `currentProfile` y `currentUsername`, y llama a `signOut`.
- `GroupsStore`, `TasksStore` y `RequestsStore` inyectan `IamStore`; `MemberTasks` y `TaskForm` leen `currentUserId`.
- `InvitationsStore` usa `IamApi.getProfileByUsername` para convertir un nombre de usuario en `userId` antes de invitar.

## Pruebas

`iam.store.spec.ts`, `iam.interceptor.spec.ts`, `iam.guard.spec.ts`, `iam.assemblers.spec.ts`, `iam.api.spec.ts`, `sign-in-page.spec.ts` y `sign-up-page.spec.ts` (Vitest). Detalle y estado en [TESTS.md](../../../../TESTS.md).
