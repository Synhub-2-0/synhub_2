# Instrucciones para agentes de código

Monorepo de **SynHub**: `frontend/` (Angular 22) y `backend/` (Spring Boot 4.1, Java 21, PostgreSQL). Antes de leer o cambiar código, usa la documentación de [`docs/`](docs/README.md) como contexto: describe cada feature, la arquitectura y quién es dueño de cada pieza.

## Por dónde empezar

1. **[`docs/README.md`](docs/README.md)**: índice general, tabla de features e índice por tema.
2. Para trabajar en una feature: `docs/features/<feature>/README.md`, y luego `backend/README.md` o `frontend/README.md` según corresponda.
   Features: `iam`, `groups`, `invitations`, `tasks`, `requests`, `ai`, `metrics`, `tour`.
3. Para decisiones de estructura: [`docs/architecture/backend.md`](docs/architecture/backend.md) y [`docs/architecture/frontend.md`](docs/architecture/frontend.md).
4. Para endpoints: [`docs/architecture/rest-api.md`](docs/architecture/rest-api.md).
5. Si el cambio toca varias features: [`docs/architecture/cross-feature-ownership.md`](docs/architecture/cross-feature-ownership.md) indica a qué contexto le pertenece.
6. Historial de calidad: [`docs/AUDITORIA.md`](docs/AUDITORIA.md) registra cada hallazgo (ID, evidencia, solución aplicada) y las decisiones de producto (`HUM-*`). Todos están resueltos; un hallazgo nuevo se agrega con el siguiente ID y el mismo formato.
7. Para pruebas: [`TESTS.md`](TESTS.md) lista cada archivo de pruebas y su estado. Para un defecto nuevo, escribe primero la prueba que lo reproduce.

## Reglas del proyecto

- **Backend**: cada contexto sigue `interfaces/rest` → `application/internal` → `domain` ← `infrastructure/persistence`. Las operaciones son `record` *Command*/*Query* manejadas por `handle(...)` en `*CommandService`/`*QueryService`. El usuario que actúa se toma del token, nunca del cuerpo. Las reglas de negocio lanzan subclases de `BusinessRuleViolationException` (409). Para usar datos de otro contexto, usa su `*ContextFacade`: nunca sus repositorios (`architecture/BoundedContextIsolationTest` lo verifica).
- **Base de datos**: el esquema lo crean las migraciones de Flyway (`backend/src/main/resources/db/migration`). Un cambio de entidad necesita su `V<n>__descripcion.sql`; Hibernate solo valida. Borrar es lógico (`@SoftDelete`, columna `deleted_at`).
- **Sesión**: cookie `HttpOnly` `SYNHUB_SESSION` (1 día); la web nunca guarda el JWT.
- **Frontend**: los componentes solo hablan con stores (`application/*.store.ts`, con signals); los stores con `*Api`; los `*Api` con `*ApiEndpoint` + assembler (`infrastructure/`). Las URLs salen de `src/environments`.
- **Invitations** se implementa en el paquete `groups` del backend; no existe un paquete `invitations`.
- `GET /groups/{id}/requests` le pertenece a **requests** (`GroupRequestController`).
- No copies las desviaciones que aún listan `docs/architecture/backend.md §10` y `frontend.md §8`.

## Comandos

```bash
cd backend && ./mvnw verify               # tests (PostgreSQL con Testcontainers: requiere Docker) + chequeo de formato
cd backend && ./mvnw spotless:apply       # aplica el formato Java (palantir-java-format)
cd frontend && npx ng lint                # ESLint (angular-eslint)
cd frontend && npx ng test --watch=false  # tests del frontend (Vitest)
docker compose up --build                 # solución completa en http://localhost:8080 (antes: cp .env.example .env)
```

## Mantener la documentación

Si cambias un endpoint, una regla de negocio, una vista o un store, actualiza el documento correspondiente en `docs/features/<feature>/` y, si aplica, `docs/architecture/rest-api.md`. Ver la sección "Mantenimiento" de [`docs/README.md`](docs/README.md#mantenimiento).
