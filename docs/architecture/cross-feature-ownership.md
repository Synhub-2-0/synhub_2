# Propiedad entre features

Varias piezas del código combinan datos o comportamiento de más de una feature: un endpoint con la URL de un recurso implementado por otro contexto, un repositorio que navega por tres agregados, una vista que usa cuatro stores. Este documento fija **a quién le pertenece cada pieza**, para saber dónde cambiarla y dónde documentarla.

## Criterios

En orden de prioridad:

1. **Dueño del invariante.** La pieza le pertenece al contexto cuya regla de negocio protege o cuyo estado modifica. *Aceptar una invitación crea una membresía* → groups.
2. **Dueño del agregado que se escribe.** Si la pieza escribe, el dueño es el contexto del agregado que persiste. Si solo lee, el dueño es el contexto que **necesita** el dato, no el que lo almacena (`TaskMetricsQueryServiceImpl` lee tareas, pero le pertenece a metrics).
3. **Dueño del recurso REST.** Para endpoints, se considera la URL **raíz** del recurso, pero cede ante los criterios 1 y 2: `/groups/{id}/requests` le pertenece a requests porque devuelve requests.
4. **En el frontend, dueño del store.** Un componente le pertenece a la feature de la carpeta donde vive; el store o API que usa determina **qué feature documenta la interacción**.

## Matriz del backend

| Pieza | Ubicación | Toca | **Dueño** | Por qué |
|---|---|---|---|---|
| `InvitationController`, `Invitation`, `Invitation*Service` | `groups` | groups, iam | **groups** (recurso REST `/invitations`) | Aceptar crea `GroupProfile`; `Group.invitations` las contiene en cascada. |
| `GroupRequestController` (`GET /groups/{id}/requests`) | `requests` | requests, groups | **requests** | Devuelve `Request`; de groups solo consulta quién es el líder, mediante ACL. |
| `RequestRepository.findByTask_Profile_Group_Id` | `requests` | requests → tasks → groups | **requests** | Consulta de requests; navega las relaciones JPA. |
| `TaskRepository.findByProfile_Group_Id…` / `…_User_Id` | `tasks` | tasks → groups → iam | **tasks** | Consulta de tareas filtrada por membresía. |
| `GroupProfile.tasks` / `Task.profile` | `groups` / `tasks` | groups ↔ tasks | **tasks** es dueño de la asignación; **groups**, de la membresía | La tarea se asigna a una membresía; la membresía no tiene reglas sobre tareas. |
| `TaskStatusScheduler` | `tasks/application/internal/schedulers` | tasks, requests | **tasks** (proceso) · **requests** (dato creado) | El vencimiento es una regla de la tarea; la request `EXPIRED` es una consecuencia. |
| `TasksContextFacade.applySubmissionReview` | `tasks` | tasks | **tasks** | Única vía hacia `DONE`; requests solo la invoca. |
| `TaskCommandServiceImpl` (usa `GroupsContextFacade` e `IamContextFacade`) | `tasks` | tasks, groups, iam | **tasks** | Escribe `Task`; la membresía y el usuario llegan por las ACL. |
| `GroupCommandServiceImpl` / `InvitationCommandServiceImpl` (usan `IamContextFacade`) | `groups` | groups, iam | **groups** | Escriben grupos y membresías. |
| `TaskMetricsQueryServiceImpl` | `metrics` | tasks, iam, groups | **metrics** | Solo lee; el cálculo es de metrics. |
| `GroupReportQueryServiceImpl` | `ai` | metrics, tasks | **ai** | Orquesta datos de otros contextos para el informe. |
| `AiController.getGroupReport` (valida al líder) | `ai` | groups, iam | **ai** | La regla "solo el líder" se *consulta* en groups, pero el caso de uso es de ai. |
| Campos de IA en `Task` (`difficulty`, `context`, `urgency`, `labels`, `aiAccepted`) | `tasks` | tasks, ai | **tasks** (persistencia) · **ai** (valores y significado) | ai sugiere y tasks guarda. |
| Resolución del usuario actual en los controladores | todos | iam | **iam** | Patrón repetido: `UserQueryService` + `GetUserByUsernameQuery`. |

## Matriz del frontend

| Pieza | Carpeta | Stores / APIs que usa | **Dueño** | Interacciones documentadas en |
|---|---|---|---|---|
| `GroupDetails` | groups | Groups, Tasks, Invitations, Iam | **groups** | [tasks](../features/tasks/frontend/README.md), [invitations](../features/invitations/frontend/README.md) |
| `MemberGroups` | groups | Groups, Invitations | **groups** | [invitations](../features/invitations/frontend/README.md) |
| `InvitationsStore` | invitations | `InvitationsApi`, `IamApi`, `IamStore` | **invitations** | [iam](../features/iam/README.md) (búsqueda por username) |
| `InvitationsAssembler` | invitations | `GroupsAssembler`, `ProfilesAssembler` | **invitations** | — |
| `TaskCard` | tasks | Requests | **tasks** | [requests](../features/requests/frontend/README.md) |
| `TaskForm` | tasks | Tasks, Groups, Iam, Ai, Toast | **tasks** | [ai](../features/ai/frontend/README.md) |
| `LeaderTasks` (incluye `AiReportCard`) | tasks | Tasks, Groups (+ Ai en la tarjeta) | **tasks** | [ai](../features/ai/frontend/README.md) |
| `AiReportCard` | ai | Ai | **ai** | — |
| `Validations` | requests | Requests, Groups | **requests** | — |
| `RequestsApiEndpoint` (URLs `/tasks/…` y `/groups/…`) | requests | — | **requests** | — |
| `RequestsAssembler` | requests | `TasksAssembler` | **requests** | — |
| `GroupsAssembler` | groups | `ProfilesAssembler` | **groups** | — |
| `Home` | shared | Groups, Tasks | **shared** | [tasks](../features/tasks/frontend/README.md) |
| `Layout` | shared | Iam | **shared** | [iam](../features/iam/frontend/README.md) |
| `ProfilesApiEndpoint` (lee `/users`) | iam | — | **iam** | — |

## Cómo documentar una pieza compartida

1. Se documenta **en detalle** en la feature dueña (backend o frontend, según corresponda).
2. En el README de cada feature que toca, se agrega una línea en **"Propiedad compartida"** con un enlace al dueño.
3. Si la propiedad cambia, se actualiza esta matriz.

## Deuda de integración

Resuelta en AUDIT ARC-01: ningún contexto usa los repositorios de otro; todo pasa por las fachadas `*ContextFacade` (`architecture/BoundedContextIsolationTest` lo verifica). Quedan, a propósito:

- Relaciones JPA entre contextos (`Task.profile → GroupProfile`, `GroupProfile.user`, `Invitation.user`): cambiarlas por ids es una refactorización mayor.
- Servicios de dominio usados directamente: `UserQueryService` en los controladores (resolver el usuario de la sesión), `GroupQueryService` y `TaskQueryService` en ai y metrics.
