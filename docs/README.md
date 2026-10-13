# Documentación de SynHub

Punto de entrada de toda la documentación del monorepo. Tiene dos partes:

- **[`architecture/`](architecture/README.md)**: cómo está organizado técnicamente el proyecto (DDD, capas tipo Clean Architecture, CQRS, REST y despliegue). Es transversal a todas las features.
- **[`features/`](#features)**: una carpeta por feature funcional, definida a partir de los recursos REST, con la descripción de qué hace y cómo se comunican el frontend y el backend.

## Cómo está organizada

```text
docs/
├── README.md                          ← este índice
├── AUDITORIA.md                       auditoría: hallazgos, soluciones aplicadas y cierre
├── architecture/
│   ├── README.md                      visión general, stack, principios, recorrido de una petición
│   ├── backend.md                     DDD, capas, CQRS, ACL, persistencia, errores, seguridad
│   ├── frontend.md                    features, capas, endpoint + assembler, stores con signals
│   ├── rest-api.md                    convenciones y catálogo de endpoints ↔ consumidores
│   ├── cross-feature-ownership.md     a quién le pertenece el código compartido
│   └── deployment.md                  Docker, variables de entorno, CI/CD
└── features/
    └── <feature>/
        ├── README.md                  qué hace, recursos REST, flujo frontend ↔ backend, propiedad compartida
        ├── backend/README.md          endpoints, modelo de dominio, servicios, reglas, errores, pruebas
        └── frontend/README.md         rutas, vistas, store, infraestructura HTTP, modelo, pruebas
```

**Cómo se definió cada feature**: hay una por recurso REST raíz o por bounded context. Cuando un recurso REST (por ejemplo, `/invitations`) se implementa dentro de otro bounded context (`groups`), igual tiene su propia carpeta, y su README indica explícitamente quién es el dueño de cada capa. Las reglas para decidirlo están en [cross-feature-ownership.md](architecture/cross-feature-ownership.md).

**Cómo leer una feature**: empezar por su `README.md` (vista funcional y tabla "acción en la web → frontend → petición → backend") y luego bajar a `backend/` o `frontend/` según la parte que se vaya a modificar.

## Features

| Feature | Qué cubre | Recurso REST | Backend (paquete) | Frontend (carpeta) | Documentos |
|---|---|---|---|---|---|
| **iam** | Registro, login con JWT, usuarios y roles de cuenta | `/authentication`, `/users`, `/roles` | `iam` | `iam/` | [README](features/iam/README.md) · [backend](features/iam/backend/README.md) · [frontend](features/iam/frontend/README.md) |
| **groups** | Grupos, líder, membresías e integrantes | `/groups` | `groups` | `groups/` | [README](features/groups/README.md) · [backend](features/groups/backend/README.md) · [frontend](features/groups/frontend/README.md) |
| **invitations** | Invitar, aceptar, rechazar y cancelar | `/invitations` | `groups` ⚠ | `invitations/` (UI en `groups/`) | [README](features/invitations/README.md) · [backend](features/invitations/backend/README.md) · [frontend](features/invitations/frontend/README.md) |
| **tasks** | Tareas, tablero Kanban, ciclo de estados y vencimiento automático | `/tasks` | `tasks` | `tasks/` | [README](features/tasks/README.md) · [backend](features/tasks/backend/README.md) · [frontend](features/tasks/frontend/README.md) |
| **requests** | Entregas de tareas y validación por el líder | `/tasks/{id}/requests`, `/groups/{id}/requests` | `requests` | `requests/` | [README](features/requests/README.md) · [backend](features/requests/backend/README.md) · [frontend](features/requests/frontend/README.md) |
| **ai** | Clasificación de tareas e informe de grupo con Bedrock | `/ai` | `ai` | `ai/` | [README](features/ai/README.md) · [backend](features/ai/backend/README.md) · [frontend](features/ai/frontend/README.md) |
| **metrics** | Cálculos de productividad (API base de solo lectura; los usa el informe de IA) | `/metrics` | `metrics` | — | [README](features/metrics/README.md) · [backend](features/metrics/backend/README.md) · [frontend](features/metrics/frontend/README.md) |
| **tour** | Video de recorrido por capítulos | — | — | `tour/` | [README](features/tour/README.md) · [backend](features/tour/backend/README.md) · [frontend](features/tour/frontend/README.md) |

⚠ = el recurso REST y el código del backend pertenecen a contextos distintos.

## Índice por tema

### Arquitectura y convenciones

| Tema | Documento |
|---|---|
| Visión general, diagrama del sistema y stack | [architecture/README.md](architecture/README.md) |
| Bounded contexts y mapa de contextos | [architecture/backend.md §1](architecture/backend.md#1-bounded-contexts-ddd-estratégico) |
| Capas del backend (interfaces / application / domain / infrastructure) | [architecture/backend.md §2](architecture/backend.md#2-capas-dentro-de-un-contexto-clean-architecture--hexagonal) |
| CQRS (comandos, consultas, `handle`) | [architecture/backend.md §3](architecture/backend.md#3-cqrs-a-nivel-de-servicio) |
| Controladores, resources y assemblers | [architecture/backend.md §4](architecture/backend.md#4-rest-controladores-resources-y-assemblers) |
| Persistencia, auditoría y nombres de tablas | [architecture/backend.md §5](architecture/backend.md#5-persistencia) |
| Manejo de errores y códigos HTTP | [architecture/backend.md §6](architecture/backend.md#6-errores) |
| Seguridad y JWT | [architecture/backend.md §7](architecture/backend.md#7-seguridad) · [iam/backend](features/iam/backend/README.md) |
| Capas del frontend | [architecture/frontend.md §2](architecture/frontend.md#2-capas-dentro-de-una-feature) |
| Patrón endpoint + assembler y entornos | [architecture/frontend.md §3](architecture/frontend.md#3-comunicación-con-el-backend-endpoint--assembler) |
| Stores con signals | [architecture/frontend.md §4](architecture/frontend.md#4-estado-stores-con-signals) |
| Rutas, guard e interceptor | [architecture/frontend.md §5](architecture/frontend.md#5-enrutamiento-sesión-y-seguridad) |
| Convenciones REST y catálogo de endpoints | [architecture/rest-api.md](architecture/rest-api.md) |
| Propiedad del código compartido | [architecture/cross-feature-ownership.md](architecture/cross-feature-ownership.md) |
| Docker, variables de entorno y CI/CD | [architecture/deployment.md](architecture/deployment.md) |
| **Auditoría: hallazgos, resolución y pendientes de despliegue** | [AUDITORIA.md](AUDITORIA.md#cierre) · prueba de humo: [`scripts/audit-smoke.py`](../scripts/audit-smoke.py) |
| **Inventario de pruebas y su estado** | [TESTS.md](../TESTS.md) |
| Desviaciones conocidas (deuda técnica) | [backend §10](architecture/backend.md#10-desviaciones-conocidas-respecto-a-la-estructura-canónica) · [frontend §8](architecture/frontend.md#8-desviaciones-conocidas) · [deuda de integración](architecture/cross-feature-ownership.md#deuda-de-integración) |

### Guías

| Tarea | Documento |
|---|---|
| Agregar un caso de uso en el backend | [architecture/backend.md §11](architecture/backend.md#11-cómo-agregar-un-caso-de-uso) |
| Agregar una feature en el frontend | [architecture/frontend.md §9](architecture/frontend.md#9-cómo-agregar-una-feature) |
| Levantar todo con Docker | [architecture/deployment.md](architecture/deployment.md#docker-solución-completa) |

### Flujos de negocio clave

| Flujo | Documento |
|---|---|
| Login y restauración de sesión | [iam](features/iam/README.md#comunicación-frontend--backend) |
| Crear un grupo e invitar integrantes | [groups](features/groups/README.md) → [invitations](features/invitations/README.md#comunicación-frontend--backend) |
| Ciclo de vida de una tarea | [tasks](features/tasks/README.md#ciclo-de-vida-de-una-tarea) |
| Entrega y validación de una tarea | [requests](features/requests/README.md#comunicación-frontend--backend) |
| Sugerencia de IA al crear una tarea | [ai](features/ai/README.md) · [tasks/frontend](features/tasks/frontend/README.md#integración-con-ia-en-taskform) |
| Informe de grupo con IA | [ai/backend](features/ai/backend/README.md#groupreportqueryserviceimpl) |

## Mantenimiento

- Al agregar o cambiar un endpoint: actualizar `features/<feature>/backend/README.md`, la tabla de comunicación del `README.md` de la feature y [rest-api.md](architecture/rest-api.md).
- Al agregar una vista o un store: actualizar `features/<feature>/frontend/README.md`.
- Si un cambio involucra varias features: documentarlo en la feature dueña y agregar una línea en "Propiedad compartida" de las demás ([criterios](architecture/cross-feature-ownership.md#criterios)).
- Al crear una feature nueva: crear `features/<feature>/{README.md,backend/README.md,frontend/README.md}` y agregarla a la tabla [Features](#features).
