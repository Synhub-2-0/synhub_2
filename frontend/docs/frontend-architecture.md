# Arquitectura del frontend de SynHub

## 1. Visión general

El frontend está construido con Angular 22, usando una estructura basada en módulos feature-first y componentes standalone. El proyecto no usa React ni un sistema de `Context` tipo React; en su lugar, la aplicación emplea Angular DI, `signal` de Angular y servicios de estado (`store`) para mantener el estado de la sesión, usuarios, grupos y tareas.

La pila principal es:

- Angular 22
- RxJS para streams y peticiones HTTP
- Angular Material para componentes UI
- Tailwind para estilos utilitarios
- Angular Router para navegación y lazy loading
- `signals` para estado reactivo local
- `localStorage` para persistencia de sesión (token y userId)

La aplicación está organizada alrededor de dominios funcionales: `iam`, `groups`, `tasks`, `invitations`, además de un layer compartido (`shared`).

## 2. Estructura de carpetas

La estructura base del frontend es la siguiente:

```text
src/
├── app/
│   ├── app.config.ts
│   ├── app.routes.ts
│   ├── app.ts
│   ├── groups/
│   │   ├── application/
│   │   ├── domain/
│   │   ├── infrastructure/
│   │   └── presentation/
│   ├── iam/
│   │   ├── application/
│   │   ├── domain/
│   │   ├── infrastructure/
│   │   └── presentation/
│   ├── invitations/
│   │   ├── application/
│   │   ├── domain/
│   │   ├── infrastructure/
│   │   └── presentation/
│   ├── shared/
│   │   ├── domain/
│   │   ├── infrastructure/
│   │   └── presentation/
│   └── tasks/
│       ├── application/
│       ├── domain/
│       ├── infrastructure/
│       └── presentation/
├── environments/
│   ├── environment.ts
│   └── environment.development.ts
├── main.ts
└── styles.css
```

### 2.1. `app/`

Es el núcleo de la aplicación. Aquí se definen:

- `app.routes.ts`: configuración principal de rutas.
- `app.config.ts`: providers globales, router y cliente HTTP.
- `app.ts`: componente raíz.

### 2.2. Domain por feature

Cada feature sigue una separación por capas:

- `domain/`: modelos, comandos y entidades.
- `application/`: stores o orchestration state.
- `infrastructure/`: clientes API, endpoints, assemblers, responses y serialización.
- `presentation/`: componentes, vistas y rutas de la feature.

Esto da una clara separación entre dominio, acceso a datos y UI.

### 2.3. `shared/`

Contiene utilidades reutilizables:

- Base classes para endpoints y assemblers.
- Layout global.
- Componentes de UI generales.
- Modelos base como `BaseEntity`.

## 3. Enrutamiento y carga por feature

La ruta raíz se define en `src/app/app.routes.ts`:

```ts
export const routes: Routes = [
  { path: 'auth', loadChildren: iamRoutes, title: `${baseTitle}`},
  { path: 'home', component: Home, title: `${baseTitle} - Home`, canActivate: [iamGuard]},
  { path: 'groups', loadChildren: groupsRoutes, title: `${baseTitle} - My Groups`, canActivate: [iamGuard]},
  { path: 'tasks', loadChildren: tasksRoutes, title: `${baseTitle} - Tasks`, canActivate: [iamGuard]},
  { path: '', redirectTo: '/auth/sign-in', pathMatch: 'full' },
  { path: '**', component: PageNotFound, title: `${baseTitle} - Page not found` }
];
```

Se observa una arquitectura típica de Angular con:

- `lazy loading` por feature mediante `loadChildren` y `loadComponent`
- protección de rutas con `canActivate: [iamGuard]`
- rutas específicas por tipo de usuario y entidad (`leader`, `member`, `create`, `:id`)

Ejemplos:

- `iam.routes.ts` define `sign-in` y `sign-up`
- `groups.routes.ts` define `leader`, `member` y detalle
- `tasks.routes.ts` define `leader`, `member`, `create` y detalle por id

Esto permite que la app no cargue todo el código al inicio y reduce el peso inicial de la aplicación.

## 4. Arquitectura de capas por feature

### 4.1. Capa `domain`

Aquí viven los modelos de negocio y los comandos de entrada:

- `group.entity.ts`
- `task.entity.ts`
- `profile.entity.ts`
- `user.entity.ts`
- `create-group.command.ts`
- `create-task.command.ts`
- `update-task.command.ts`
- `sign-in.command.ts`
- `sign-up.command.ts`

La idea es que esta capa represente el “modelo del negocio” y no dependa directamente de HTTP ni de Angular.

### 4.2. Capa `infrastructure`

Contiene los adaptadores para consumir la API externa. Hay dos niveles bien marcados:

- `*ApiEndpoint`: define las rutas y los métodos REST
- `*Api`: encapsula esos endpoints y los expone al store

Ejemplo en `groups/infrastructure/groups.api-endpoint.ts`:

```ts
const groupsEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderGroupsEndpointPath}`;

export class GroupsApiEndpoint extends BaseApiEndpoint<...> {
  constructor(http: HttpClient) {
    super(http, groupsEndpointUrl, new GroupsAssembler());
  }
}
```

Se usa una base común para manejar:

- GET all
- GET by id
- POST create
- PUT update
- DELETE delete

Esto evita repetir lógica de manejo de errores y transformación de recursos.

### 4.3. Capa `application`

La capa `application` es la más importante para el estado reactivo del frontend. Aquí se definen los stores de Angular usando `signal`.

Ejemplos:

- `IamStore`
- `GroupsStore`
- `TasksStore`

#### `IamStore`

`IamStore` centraliza la identidad del usuario autenticado:

```ts
private readonly isSignedInSignal = signal(!!localStorage.getItem('token'));
private readonly currentUserIdSignal = signal<number | null>(...);
private readonly currentProfileSignal = signal<Profile | null>(null);
```

Además ofrece:

- `signIn(command, router)`
- `signUp(command, router)`
- `signOut(router)`
- `restoreSession()`
- `clearSession()`

La sesión se persiste en `localStorage` y se restaura al arrancar la app con `provideAppInitializer`:

```ts
provideAppInitializer(() => {
  const iamStore = inject(IamStore);
  return iamStore.restoreSession();
})
```

#### `GroupsStore`

La lógica de grupos incluye:

- carga de grupos del usuario
- separación por rol (`GROUP_LEADER` / `GROUP_MEMBER`)
- cálculo de conteos con `computed`
- manejo de loading/error
- actualización local del estado después de crear o modificar un grupo

#### `TasksStore`

La lógica de tareas sigue una estructura equivalente:

- `tasksSignal`, `selectedTaskSignal`, `statusFilterSignal`
- `filteredTasks` calculado con `computed`
- `loadTasksByGroup`, `loadTasksByGroupAndUser`, `loadTaskById`
- `addTask`, `updateTask`, `updateTaskStatus`, `deleteTask`

Esto encaja muy bien con el patrón “store + signal + RxJS subscription” que usa Angular moderno.

## 5. Integración con APIs y serialización

La comunicación HTTP se apoya en un patrón de `assembler` + `response` + `resource` + `entity`.

### 5.1. `BaseApiEndpoint`

`shared/infrastructure/base-api-endpoint.ts` es una base reutilizable para cualquier recurso. Define helpers para:

- `getAll()`
- `getById(id)`
- `create(entity)`
- `update(entity, id)`
- `delete(id)`

También centraliza el manejo de errores:

```ts
protected handleError(operation: string) {
  return (error: HttpErrorResponse): Observable<never> => {
    // transforma errores HTTP en Error con mensajes consolidados
  };
}
```

### 5.2. `BaseAssembler`

Las clases `Assembler` toman un `resource` del backend y lo convierten en una entidad del dominio, y viceversa. Esto permite que el frontend use objetos tipados de negocio en vez de estructuras crudas de API.

Ejemplo: `GroupsAssembler` o `SignInAssembler` convierten JSON del backend a modelos del dominio.

### 5.3. `environment.ts`

Las URLs base del backend se centralizan en `src/environments/environment.ts`:

```ts
export const environment = {
  production: true,
  platformProviderApiBaseUrl: 'http://localhost:8080/api/v1',
  platformProviderGroupsEndpointPath: '/groups',
  platformProviderTasksEndpointPath: '/tasks',
  platformProviderInvitationsEndpointPath: '/invitations',
};
```

Esto facilita cambiar el backend o el entorno sin tocar cada endpoint individual.

## 6. Manejo de sesión y autenticación

La autenticación se gestiona principalmente en `IamStore` y en el interceptor HTTP.

### 6.1. Interceptor HTTP

`iam.interceptor.ts` añade el token JWT a todas las requests excepto las de login y signup:

```ts
if (
  request.url.includes('/authentication/sign-in') ||
  request.url.includes('/authentication/sign-up')
) {
  return next(request);
}

const token = localStorage.getItem('token');
if (!token || token === 'undefined' || token === 'null') {
  return next(request);
}

return next(request.clone({
  setHeaders: { Authorization: `Bearer ${token}` },
}));
```

Esto centraliza la autenticación del cliente sin duplicar lógica en cada llamada.

### 6.2. Guard de rutas

`iam.guard.ts` protege las páginas privadas:

```ts
export const iamGuard: CanActivateFn = (route, state) => {
  const store = inject(IamStore);
  const router = inject(Router);

  if (store.isSignedIn()) return true;
  else {
    router.navigate(['/auth/sign-in']);
    return false;
  }
};
```

Esto hace que la navegación por rutas privadas dependa del estado actual autenticado, no solo de la UI.

## 7. Uso del contexto / estado global

No existe una implementación de `Context` como en React. La arquitectura del proyecto se apoya en:

- `inject()` para obtener dependencias del contenedor Angular
- `signal` para estado reactivo local a cada store
- `@Injectable({ providedIn: 'root' })` para servicios singleton globales
- `localStorage` para persistencia de la sesión

Por lo tanto, la “contextualización” del usuario y el estado global se resuelve con stores, no con un `Provider` explícito. El equivalente funcional a un contexto global es `IamStore`, que expone señales como:

- `isSignedIn`
- `currentUsername`
- `currentUserId`
- `currentProfile`
- `currentToken`

Y otras features usan `inject(IamStore)` para consumir ese estado.

Ejemplo en `shared/presentation/components/layout/layout.ts`:

```ts
readonly iamStore = inject(IamStore);
```

Esto demuestra que la app usa DI para acceder al “estado compartido” de forma centralizada y bien acoplada.

## 8. Patrón de presentación

Los componentes siguen una estructura bastante moderna de Angular:

- `standalone: true`
- imports explícitos de módulos y dependencias
- uso de `inject()` cuando se necesita acceso a store o router
- señales para estado local del componente

Ejemplo en `layout.ts`:

```ts
readonly sidebarOpen = signal(true);
readonly isAuthRoute = signal(true);
```

La UI se compone de componentes encapsulados, reutilizables y conectados a stores. El layout global se usa como shell de la app y varía según la ruta.

## 9. Prácticas de diseño que usa el proyecto

### 9.1. Separación de responsabilidades

Cada feature está dividida en capas; no se mezclan servicios, modelos y vistas en el mismo archivo.

### 9.2. Lazy loading

Se usa para cargar rutas por secciones, como autenticación, grupos y tareas. Esto es una buena práctica para reducir el bundle inicial.

### 9.3. Estado reactivo con signals

La aplicación está migrando y usando el patrón moderno de Angular con `signal` y `computed` para reactividad y derivación de estado.

### 9.4. Persistencia de sesión

`localStorage` se usa para guardar el token JWT y el `userId`, lo que facilita la continuidad de sesión al recargar la página.

### 9.5. Transformación DTO -> entidad

El backend devuelve recursos con una estructura de respuesta. El frontend transforma esos recursos con assemblers para trabajar con entidades del dominio, no con JSON crudo.

### 9.6. Dependencias por feature

Cada feature importa lo necesario de su propio dominio y de `shared` cuando hay semántica reutilizable. El acoplamiento está localizado.

## 10. Observaciones de arquitectura

El proyecto demuestra una buena organización general, pero también presenta algunos puntos de mejora:

1. `BaseApi` está definido pero casi vacío, lo cual hace que su utilidad sea más conceptual que funcional.
2. `GroupsStore` y `TasksStore` tienen lógica de negocio y transformación de errores mezcladas con la gestión del estado, algo típico en una primera versión.
3. `loadUsers()` en `IamStore` aparece incompleto o un placeholder, lo que sugiere que ciertas partes aún no están finalizadas.
4. La sesión está gestionada con `localStorage`, lo cual es funcional, pero no es la opción más segura si se pretende una gestión avanzada de autorización o expiración.
5. Hay una mezcla de `signals`, `RxJS` y navegación imperativa con `router.navigate(...)`, que es válida en Angular pero requiere disciplina para evitar efectos secundarios dispersos.

## 11. Conclusión

El frontend de SynHub sigue un patrón Angular moderno, bien estructurado por features y con una separación clara entre dominio, infraestructura, aplicación y presentación. La decisión de usar stores basados en `signal` y servicios inyectables es coherente con la filosofía de Angular 22, y el uso de lazy loading y `HttpInterceptor` demuestra una buena práctica de modularización y seguridad.

La aplicación no usa un contexto React-like; su equivalente funcional es la combinación de:

- `IamStore` como estado global de autenticación
- servicios singleton inyectados con `providedIn: 'root'`
- `signals` para reactividad local y compartida
- `localStorage` para persistencia de sesión

En conjunto, la arquitectura es clara, mantenible y adecuada para proyectos de mediana escala con múltiples features y lógica de negocio de dominio.
