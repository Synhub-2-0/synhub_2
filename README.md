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

Orden: PostgreSQL corriendo → backend (terminal 1) → frontend (terminal 2).

### Backend: variables de entorno

Solo el backend usa variables de entorno. Spring **no lee `.env` por sí solo**: hay que cargarlo en la terminal (o en el IDE) antes de arrancar.

1. Crea **un solo** `.env` en la **raíz del repo**, junto a `.env.example` y `docker-compose.yml`. No va dentro de `backend/` ni de `frontend/`. Está en `.gitignore`, así que no se sube:

   ```text
   synhub_2/
   ├── .env            ← aquí
   ├── .env.example
   ├── docker-compose.yml
   ├── backend/
   └── frontend/
   ```

   ```powershell
   # desde la raíz del repo
   Copy-Item .env.example .env      # bash: cp .env.example .env
   ```

2. Complétalo, por ejemplo:

   ```properties
   DATABASE_URL=jdbc:postgresql://localhost:5432/postgres
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=tu_password_de_postgres
   JWT_SECRET=al_menos_32_caracteres_aleatorios_xxxxxxxxxx
   AUTH_COOKIE_SECURE=false
   CORS_ALLOWED_ORIGINS=http://localhost:4200
   AWS_REGION=us-east-1
   AWS_ACCESS_KEY_ID=
   AWS_SECRET_ACCESS_KEY=
   ```

   Para generar un `JWT_SECRET`: `openssl rand -base64 48`, o en PowerShell
   `[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))`.

| Variable | Obligatoria | Por defecto |
|---|---|---|
| `DATABASE_URL` | no | `jdbc:postgresql://localhost:5432/postgres` |
| `DATABASE_USERNAME` | no | `postgres` |
| `DATABASE_PASSWORD` | **sí**, salvo con el perfil `dev` | `dev`: `admin` |
| `JWT_SECRET` | **sí**, salvo con el perfil `dev` | `dev`: valor local |
| `AUTH_COOKIE_SECURE` | no | `true` (`dev`: `false`) |
| `CORS_ALLOWED_ORIGINS` | no | vacío (`dev`: `http://localhost:4200`) |
| `AWS_REGION` / `BEDROCK_MODEL_ID` | no | `us-east-1` / Claude Haiku |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | solo para las funciones de IA | — |
| `TASKS_EXPIRATION_CHECK_MS` | no | `60000` |

3. Carga el `.env` y arranca con el perfil `dev` ([`application-dev.properties`](backend/src/main/resources/application-dev.properties)). Ejecuta estos comandos **desde la raíz del repo**, porque leen `.env` desde ahí y luego entran a `backend/`:

   **PowerShell**

   ```powershell
   Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
     $k, $v = $_ -split '=', 2
     Set-Item "env:$($k.Trim())" $v.Trim()
   }
   $env:SPRING_PROFILES_ACTIVE = "dev"
   cd backend
   .\mvnw spring-boot:run
   ```

   **Git Bash**

   ```bash
   set -a; source .env; set +a
   cd backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   ```

   Las variables solo viven en esa terminal: en una nueva hay que volver a cargarlas. En IntelliJ, ponlas en *Run → Edit Configurations → Environment variables* (o con el plugin EnvFile apuntando a `.env`) y escribe `dev` en *Active profiles*.

   El log debe mostrar `The following 1 profile is active: "dev"`. La API queda en http://localhost:8080 y Swagger en http://localhost:8080/swagger-ui.html.

### Frontend

Angular no usa `.env`: la URL del backend se fija al compilar en [`frontend/src/environments/`](frontend/src/environments).

- `environment.development.ts` (`ng serve`): `http://localhost:8080/api/v1`. Cámbialo si el backend corre en otro host o puerto.
- `environment.ts` (`ng build`, producción): `/api/v1`, mismo origen detrás de nginx o CloudFront.

```powershell
cd frontend
npm ci
npx ng serve      # http://localhost:4200
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
