# SynHub: backend

API REST en Spring Boot 4 y Java 21, con PostgreSQL. Está organizada en bounded contexts (DDD) con capas de Clean Architecture. Es parte del monorepo: la visión general está en el [README principal](../README.md).

## Comandos

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # http://localhost:8080, Swagger en /swagger-ui.html
./mvnw -B verify                                    # pruebas; las de integración usan Testcontainers (requiere Docker)
```

El esquema de la base lo crean las migraciones de Flyway (`src/main/resources/db/migration`); un cambio de entidad necesita su migración nueva. Sin el perfil `dev`, la aplicación necesita `DATABASE_PASSWORD` y `JWT_SECRET`. Ver [docs/architecture/deployment.md](../docs/architecture/deployment.md).

## Documentación

- Arquitectura (DDD, capas, CQRS, REST, persistencia, seguridad): [docs/architecture/backend.md](../docs/architecture/backend.md) y [docs/architecture/rest-api.md](../docs/architecture/rest-api.md).
- Cada feature tiene su parte de backend en `docs/features/<feature>/backend/README.md`. El índice está en [docs/README.md](../docs/README.md).
- Pruebas y su estado: [TESTS.md](../TESTS.md).
