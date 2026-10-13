# IAM: frontend

Carpeta: [`frontend/src/app/iam`](../../../../frontend/src/app/iam)

## Rutas y vistas

| Ruta | Componente | Qué hace |
|---|---|---|
| `/auth/sign-in` | `SignInPage` (`presentation/views/sign-in-page`) | Formulario de login. Llama a `IamStore.signIn` y, si sale bien, navega a `/home`. |
| `/auth/sign-up` | `SignUpPage` (`presentation/views/sign-up-page`) | Formulario de registro. Envía `accountRoles: ['ROLE_USER']` y luego redirige a `/auth/sign-in`. |
| `''` | — | Redirige a `/auth/sign-in` (definido en `app.routes.ts`). |

Las rutas de `iam.routes.ts` se cargan de forma diferida (`loadChildren`) y **no** tienen guard.

## Estado: `IamStore` (`application/iam.store.ts`)

Store singleton (`providedIn: 'root'`) basado en signals. Funciona como "contexto global" de la sesión.

| Signal | Origen |
|---|---|
| `isSignedIn` | `!!localStorage.token` al iniciar; `true` tras el login. |
| `currentUserId` | `localStorage.userId`. |
| `currentUsername`, `currentProfile`, `currentToken` | Respuesta del login, o de `/users/me` al restaurar la sesión. |

| Método | Peticiones | Efecto |
|---|---|---|
| `signIn(command, router)` | `POST /authentication/sign-in` → `GET /users/{id}` | Guarda `token` y `userId` en `localStorage`, carga el perfil y navega a `/home`. |
| `signUp(command, router)` | `POST /authentication/sign-up` | Navega a `/auth/sign-in`. |
| `signOut(router)` | — | Borra la sesión y navega a `/auth/sign-in`. |
| `restoreSession()` | `GET /users/me` → `GET /users/{id}` | Se ejecuta en `provideAppInitializer` (`app.config.ts`), antes del primer render. |
| `loadUsers()` | — | Incompleto: solo activa `loadingUsers`. |

## Infraestructura

| Archivo | Rol |
|---|---|
| `iam.api.ts` (`IamApi`) | Fachada que compone cuatro endpoints: sign-in, sign-up, users y profiles. |
| `sign-in/*`, `sign-up/*` | `*ApiEndpoint` (extienden `ErrorHandlingEnabledBaseType`), `*Assembler`, `*Request`, `*Response`. |
| `users/*` | `UsersApiEndpoint` (`BaseApiEndpoint`): `getByAuthentication()` → `/users/me`, `getByUsername()`. |
| `profiles/*` | `ProfilesApiEndpoint`: construye un `Profile` a partir de `/users/{id}` o `/users/username`. **No existe un recurso `/profiles` en el backend**: el perfil se arma con el `UserResource`. |
| `iam.interceptor.ts` | `HttpInterceptorFn`: agrega `Authorization: Bearer <token>` a todas las peticiones salvo sign-in y sign-up. Se registra en `app.config.ts`. |
| `iam.guard.ts` | `CanActivateFn`: si `isSignedIn()` es falso, redirige a `/auth/sign-in`. Protege `home`, `groups`, `tasks`, `validations` y `recorrido`. |

## Modelo

| Clase | Campos |
|---|---|
| `User` | `id`, `username`, `accountRoles` |
| `Profile` | `id`, `name`, `surname`, `email`, `imgUrl`, `userId`, `username`. `userId` queda `undefined` porque `UserResource` no lo envía; `id` es el id del usuario. |
| `SignInCommand`, `SignUpCommand` | Datos de los formularios. |

## Consumidores fuera de IAM

- `Layout` (shared): muestra `currentProfile` y `currentUsername`, y llama a `signOut`.
- `GroupsStore`, `TasksStore` y `RequestsStore` inyectan `IamStore`; `MemberTasks` y `TaskForm` leen `currentUserId`.
- `InvitationsStore` usa `IamApi.getProfileByUsername` para convertir un nombre de usuario en `userId` antes de invitar.

## Pruebas

`sign-in-page.spec.ts` y `sign-up-page.spec.ts` (Vitest).
