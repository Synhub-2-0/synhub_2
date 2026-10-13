# Instrucciones para agentes de código

Monorepo de **SynHub**: `frontend/` (Angular 22) y `backend/` (Spring Boot 4.1, Java 21, PostgreSQL). Antes de leer o cambiar código, usa la documentación de [`docs/`](docs/README.md) como contexto: describe cada feature, la arquitectura y quién es dueño de cada pieza.

## Por dónde empezar

1. **[`docs/README.md`](docs/README.md)**: índice general, tabla de features e índice por tema.
2. Para trabajar en una feature: `docs/features/<feature>/README.md`, y luego `backend/README.md` o `frontend/README.md` según corresponda.
   Features: `iam`, `groups`, `invitations`, `tasks`, `requests`, `ai`, `metrics`, `tour`.
3. Para decisiones de estructura: [`docs/architecture/backend.md`](docs/architecture/backend.md) y [`docs/architecture/frontend.md`](docs/architecture/frontend.md).
4. Para endpoints: [`docs/architecture/rest-api.md`](docs/architecture/rest-api.md).
5. Si el cambio toca varias features: [`docs/architecture/cross-feature-ownership.md`](docs/architecture/cross-feature-ownership.md) indica a qué contexto le pertenece.

## Reglas del proyecto

- **Backend**: cada contexto sigue `interfaces/rest` → `application/internal` → `domain` ← `infrastructure/persistence`. Las operaciones son `record` *Command*/*Query* manejadas por `handle(...)` en `*CommandService`/`*QueryService`. El usuario que actúa se toma del token, nunca del cuerpo. Las reglas de negocio lanzan subclases de `BusinessRuleViolationException` (409). Para usar datos de otro contexto, usa su `*ContextFacade` en lugar de sus repositorios.
- **Frontend**: los componentes solo hablan con stores (`application/*.store.ts`, con signals); los stores con `*Api`; los `*Api` con `*ApiEndpoint` + assembler (`infrastructure/`). Las URLs salen de `src/environments`.
- **Invitations** se implementa en el paquete `groups` del backend; no existe un paquete `invitations`.
- `GET /groups/{id}/requests` le pertenece a **requests** (`GroupRequestController`).
- No copies las desviaciones listadas en `docs/architecture/backend.md §10` y `frontend.md §8` (erratas de paquetes, accesos directos a repositorios ajenos, etc.).

## Comandos

```bash
cd backend && ./mvnw verify               # tests del backend (requiere PostgreSQL en localhost:5432)
cd frontend && npx ng test --watch=false  # tests del frontend (Vitest)
docker compose up --build                 # solución completa en http://localhost:8080
```

## Mantener la documentación

Si cambias un endpoint, una regla de negocio, una vista o un store, actualiza el documento correspondiente en `docs/features/<feature>/` y, si aplica, `docs/architecture/rest-api.md`. Ver la sección "Mantenimiento" de [`docs/README.md`](docs/README.md#mantenimiento).
