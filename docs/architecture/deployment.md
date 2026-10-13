# Despliegue, entornos y CI/CD

## Desarrollo local

```bash
# Backend (requiere PostgreSQL en localhost:5432)
cd backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Frontend (usa environment.development.ts → http://localhost:8080/api/v1)
cd frontend && npm ci && npx ng serve
```

## Docker: solución completa

| Archivo | Rol |
|---|---|
| [`Dockerfile`](../../Dockerfile) | Imagen única con tres etapas: `node:22-alpine` compila Angular, `eclipse-temurin:21-jdk` empaqueta el jar (sin tests) y el runtime `eclipse-temurin:21-jre` + nginx. |
| [`docker/nginx.conf`](../../docker/nginx.conf) | Sirve la SPA (fallback a `index.html`) y reenvía `/api/` y `/actuator/health` a `127.0.0.1:8080` (con `Host: $http_host`). Agrega cabeceras de seguridad (CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy`) y limita `sign-in`/`sign-up` a 5 por minuto por IP (ráfaga de 10; luego 429). |
| [`docker/entrypoint.sh`](../../docker/entrypoint.sh) | Arranca Java (como usuario `app`) y nginx; si uno de los dos termina, el contenedor se detiene. |
| [`docker-compose.yml`](../../docker-compose.yml) | `db` (postgres:16 con volumen `pgdata`) + `app` (puerto `8080:80`). |
| [`.dockerignore`](../../.dockerignore), [`.gitattributes`](../../.gitattributes) | Excluye artefactos y fuerza finales de línea LF en los scripts del contenedor. |

```bash
cp .env.example .env        # completa DATABASE_PASSWORD y JWT_SECRET
docker compose up --build   # http://localhost:8080
```

## Variables de entorno del backend

Perfiles:

- **Base (`application.properties`):** segura por defecto. Sin secretos en el repositorio (todo llega por variables de entorno), Swagger apagado y CORS solo para los orígenes de `CORS_ALLOWED_ORIGINS` (vacío = solo mismo origen).
- **`prod`** (`SPRING_PROFILES_ACTIVE=prod`): lo fijan el `Dockerfile` raíz, `backend/Dockerfile` y compose. Solo apaga `show-sql`. En ECS se define en la task definition (HUM-02).
- **`dev`** (`SPRING_PROFILES_ACTIVE=dev`): `application-dev.properties` define una contraseña de base y un `JWT_SECRET` locales, enciende Swagger (`/swagger-ui.html`) y permite CORS desde `http://localhost:4200`. Nunca usarlo en un entorno desplegado.
- **Pruebas:** `src/test/resources/config/application.properties` define un secreto de prueba; la base la pone Testcontainers.


| Variable | Propiedad | Por defecto | Notas |
|---|---|---|---|
| `DATABASE_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/postgres` | — |
| `DATABASE_USERNAME` | `spring.datasource.username` | `postgres` | — |
| `DATABASE_PASSWORD` | `spring.datasource.password` | **sin valor por defecto** (el perfil `dev` trae uno local) | Obligatorio. |
| `JWT_SECRET` | `authorization.jwt.secret` | **sin valor por defecto** (el perfil `dev` trae uno local) | Obligatorio, al menos 32 bytes: si falta o es más corto, la app no arranca. En AWS se inyecta desde SSM. |
| `AUTH_COOKIE_SECURE` | `authorization.cookie.secure` | `true` (`false` en `dev` y en compose) | Atributo `Secure` de la cookie de sesión. Debe ser `true` detrás de HTTPS (AWS). |
| `TASKS_EXPIRATION_CHECK_MS` | `app.tasks.expiration-check-ms` | `60000` | Cada cuánto se vencen las tareas atrasadas. |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | vacío (`http://localhost:4200` en `dev`) | Orígenes separados por comas. En producción no hace falta: front y API comparten origen. |
| `AWS_REGION` | `ai.bedrock.region` | `us-east-1` | — |
| `BEDROCK_MODEL_ID` | `ai.bedrock.model-id` | Claude Haiku 4.5 (perfil `us.`) | — |
| Credenciales de AWS | — | cadena por defecto del SDK | Necesarias solo para las funciones de IA. |
| `RUN_BEDROCK_IT=1` | — | — | Activa la prueba de integración con Bedrock. |

**Sesión por cookie (HUM-04).** La web autentica con la cookie `HttpOnly` `SYNHUB_SESSION` (`Path=/api`, `SameSite=Strict`, 1 día). Funciona porque front y API comparten origen. En CloudFront, el *behavior* de `/api/*` debe **reenviar las cookies** al origen (política de *origin request* `AllViewer`, o al menos la cookie `SYNHUB_SESSION`) y **no cachear** esas respuestas; si no, toda petición autenticada responde 401 (ver HUM-02).

## CI/CD (GitHub Actions)

Los workflows están en [`.github/workflows`](../../.github/workflows) y cada uno se dispara **solo si cambian archivos de su carpeta** (filtro `paths`).

| Workflow | Disparador | Qué hace |
|---|---|---|
| `frontend-deploy.yml` | PR y push a `main` sobre `frontend/**` | `npm ci`, `npm audit --audit-level=high`, `ng test` y `ng build`. En `main`, y solo si el build pasó (`needs: build`): `aws s3 sync` al bucket `synhub-front-<cuenta>` e invalidación de CloudFront si está encendido. |
| `backend.yml` | PR y push a `main` sobre `backend/**` | Job `test`: `./mvnw -B verify` (PostgreSQL con Testcontainers; el runner trae Docker) y guarda el jar. Job `deploy` (`needs: test`, solo push a `main`): construye la imagen `linux/arm64` con ese mismo jar y el `backend/Dockerfile`, la sube a ECR (`synhub-backend:latest` y `:<sha>`) y fuerza un redeploy en ECS si el servicio está activo. Un test en rojo detiene el deploy. |

[`.github/dependabot.yml`](../../.github/dependabot.yml) revisa cada semana las dependencias de npm, Maven, GitHub Actions y las imágenes Docker.

Requisitos en el repositorio de GitHub: variables `AWS_DEPLOY_ROLE_ARN` y `AWS_ACCOUNT_ID`, y un rol IAM con *trust policy* OIDC para `repo:Synhub-2-0/synhub_2:ref:refs/heads/main`.

> El `Dockerfile` de la raíz sirve para ejecutar la solución completa (local, demos o un único host). El despliegue en AWS sigue usando `backend/Dockerfile` (solo el jar) y S3/CloudFront para el frontend.
