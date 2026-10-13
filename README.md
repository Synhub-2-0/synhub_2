# SynHub 2.0

SynHub es una aplicación de gestión ágil de tareas para equipos. Un **líder** crea un grupo, invita a los integrantes y les asigna tareas; los integrantes avanzan sus tareas en un tablero Kanban y envían una **entrega** que el líder valida. Además, la **IA** (Amazon Bedrock) sugiere la clasificación de cada tarea y genera informes de carga del equipo.

Este monorepo reúne las dos aplicaciones (con el historial de commits de ambos repos originales):

| Carpeta | Contenido | Repo original |
|---|---|---|
| [`frontend/`](frontend) | SPA en Angular 22 (signals, Angular Material, Tailwind) | [Synhub-Web-App-v2](https://github.com/Synhub-2-0/Synhub-Web-App-v2) |
| [`backend/`](backend) | API REST en Spring Boot 4.1 / Java 21 / PostgreSQL | [synhub_backend](https://github.com/Synhub-2-0/synhub_backend) |
| [`docs/`](docs/README.md) | Documentación funcional y técnica | — |

## Documentación

Toda la documentación está en **[`docs/`](docs/README.md)** y se divide en dos partes:

- **[`docs/architecture/`](docs/architecture/README.md)**: organización técnica del proyecto. El backend aplica **DDD** con un bounded context por feature y capas al estilo **Clean Architecture / hexagonal** (`interfaces → application → domain ← infrastructure`), **CQRS** a nivel de servicio y **ACL** entre contextos. El frontend replica esos contextos como módulos con capas `domain / infrastructure / application / presentation`, stores con **signals** y un patrón **endpoint + assembler** para consumir la API **REST**. También incluye el catálogo de endpoints, los criterios de propiedad del código compartido y el despliegue.
- **[`docs/features/<feature>/`](docs/README.md#features)**: una carpeta por feature, definida según los recursos REST. Cada una tiene un `README.md` (qué hace y cómo se comunican frontend y backend), `backend/README.md` y `frontend/README.md`.

Además, **[`docs/AUDIT_RESULTS.md`](docs/AUDIT_RESULTS.md)** resume la auditoría técnica: qué se corrigió y qué queda pendiente antes del despliegue.

### Features

| Feature | Resumen | Docs |
|---|---|---|
| iam | Registro, login JWT, usuarios y roles | [README](docs/features/iam/README.md) · [backend](docs/features/iam/backend/README.md) · [frontend](docs/features/iam/frontend/README.md) |
| groups | Grupos, líder, membresías | [README](docs/features/groups/README.md) · [backend](docs/features/groups/backend/README.md) · [frontend](docs/features/groups/frontend/README.md) |
| invitations | Invitaciones a grupos (backend dentro de `groups`) | [README](docs/features/invitations/README.md) · [backend](docs/features/invitations/backend/README.md) · [frontend](docs/features/invitations/frontend/README.md) |
| tasks | Tareas, Kanban, ciclo de estados, vencimiento automático | [README](docs/features/tasks/README.md) · [backend](docs/features/tasks/backend/README.md) · [frontend](docs/features/tasks/frontend/README.md) |
| requests | Entregas y validación por el líder | [README](docs/features/requests/README.md) · [backend](docs/features/requests/backend/README.md) · [frontend](docs/features/requests/frontend/README.md) |
| ai | Clasificación de tareas e informe de grupo | [README](docs/features/ai/README.md) · [backend](docs/features/ai/backend/README.md) · [frontend](docs/features/ai/frontend/README.md) |
| metrics | Métricas de productividad (API base de solo lectura) | [README](docs/features/metrics/README.md) · [backend](docs/features/metrics/backend/README.md) · [frontend](docs/features/metrics/frontend/README.md) |
| tour | Video de recorrido por capítulos (solo frontend) | [README](docs/features/tour/README.md) · [backend](docs/features/tour/backend/README.md) · [frontend](docs/features/tour/frontend/README.md) |

### Documentación técnica

| Documento | Contenido |
|---|---|
| [Visión general](docs/architecture/README.md) | Diagrama del sistema, stack, principios y recorrido de una petición |
| [Backend](docs/architecture/backend.md) | Bounded contexts, mapa de contextos, capas, CQRS, REST, persistencia, errores, seguridad |
| [Frontend](docs/architecture/frontend.md) | Módulos por feature, capas, endpoint + assembler, stores, rutas, guard e interceptor |
| [API REST](docs/architecture/rest-api.md) | Convenciones y catálogo de endpoints con su consumidor en el frontend |
| [Propiedad entre features](docs/architecture/cross-feature-ownership.md) | A quién le pertenece el código que mezcla varias features |
| [Despliegue](docs/architecture/deployment.md) | Docker, variables de entorno, CI/CD |

El inventario de pruebas y su estado está en [`TESTS.md`](TESTS.md).

Los agentes de código (Claude Code, Gemini CLI, Codex) tienen sus instrucciones en [`CLAUDE.md`](CLAUDE.md), [`GEMINI.md`](GEMINI.md) y [`AGENTS.md`](AGENTS.md), que remiten a esta documentación.

## Desarrollo

```bash
# Backend (requiere PostgreSQL en localhost:5432)
cd backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # el perfil dev trae credenciales locales

# Frontend → http://localhost:4200 (llama a http://localhost:8080/api/v1)
cd frontend && npm ci && npx ng serve
```

Pruebas:

```bash
cd backend && ./mvnw verify          # necesita Docker (Testcontainers) o DATABASE_URL
cd frontend && npx ng test --watch=false
```

## Docker (solución completa)

El `Dockerfile` de la raíz genera una sola imagen: compila el frontend y el backend, y en ejecución nginx sirve la web en el puerto 80 y reenvía `/api/` al backend (Spring Boot, puerto interno 8080).

```bash
cp .env.example .env   # completa DATABASE_PASSWORD y JWT_SECRET
docker compose up --build
```

Esto levanta la app y PostgreSQL; la web queda en http://localhost:8080. Variables de entorno y detalles en [docs/architecture/deployment.md](docs/architecture/deployment.md).

## CI/CD

Los workflows están en `.github/workflows/` y solo se disparan cuando cambian archivos de su carpeta:

- `frontend-deploy.yml`: `npm audit`, tests y build del frontend; en `main`, si pasan, despliega a S3/CloudFront.
- `backend.yml`: `mvnw verify` (PostgreSQL con Testcontainers); en `main`, si pasan, construye la imagen con el jar probado, la sube a ECR y redepliega en ECS.
- `dependabot.yml`: actualizaciones semanales de dependencias.
