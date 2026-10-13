# Despliegue, entornos y CI/CD

## Desarrollo local

```bash
# Backend (requiere PostgreSQL en localhost:5432)
cd backend && ./mvnw spring-boot:run

# Frontend (usa environment.development.ts → http://localhost:8080/api/v1)
cd frontend && npm ci && npx ng serve
```

## Docker: solución completa

| Archivo | Rol |
|---|---|
| [`Dockerfile`](../../Dockerfile) | Imagen única con tres etapas: `node:22-alpine` compila Angular, `eclipse-temurin:21-jdk` empaqueta el jar (sin tests) y el runtime `eclipse-temurin:21-jre` + nginx. |
| [`docker/nginx.conf`](../../docker/nginx.conf) | Sirve la SPA (fallback a `index.html`) y reenvía `/api/` y `/actuator/health` a `127.0.0.1:8080`. |
| [`docker/entrypoint.sh`](../../docker/entrypoint.sh) | Arranca Java (como usuario `app`) y nginx; si uno de los dos termina, el contenedor se detiene. |
| [`docker-compose.yml`](../../docker-compose.yml) | `db` (postgres:16 con volumen `pgdata`) + `app` (puerto `8080:80`). |
| [`.dockerignore`](../../.dockerignore), [`.gitattributes`](../../.gitattributes) | Excluye artefactos y fuerza finales de línea LF en los scripts del contenedor. |

```bash
docker compose up --build   # http://localhost:8080
```

## Variables de entorno del backend

| Variable | Propiedad | Por defecto | Notas |
|---|---|---|---|
| `DATABASE_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/postgres` | — |
| `DATABASE_USERNAME` | `spring.datasource.username` | `postgres` | — |
| `DATABASE_PASSWORD` | `spring.datasource.password` | valor de desarrollo | — |
| `JWT_SECRET` | `authorization.jwt.secret` | valor de desarrollo | **Obligatorio cambiarlo fuera de desarrollo**; en AWS se inyecta desde SSM. |
| `AWS_REGION` | `ai.bedrock.region` | `us-east-1` | — |
| `BEDROCK_MODEL_ID` | `ai.bedrock.model-id` | Claude Haiku 4.5 (perfil `us.`) | — |
| Credenciales de AWS | — | cadena por defecto del SDK | Necesarias solo para las funciones de IA. |
| `RUN_BEDROCK_IT=1` | — | — | Activa la prueba de integración con Bedrock. |

## CI/CD (GitHub Actions)

Los workflows están en [`.github/workflows`](../../.github/workflows) y cada uno se dispara **solo si cambian archivos de su carpeta** (filtro `paths`).

| Workflow | Disparador | Qué hace |
|---|---|---|
| `frontend-deploy.yml` | PR y push a `main` sobre `frontend/**` | `npm ci`, `ng test` y `ng build`. En `main`: `aws s3 sync` al bucket `synhub-front-<cuenta>` e invalidación de CloudFront si está encendido. |
| `backend-ci.yml` | PR y push a `main` sobre `backend/**` | `./mvnw -B verify` con un servicio `postgres:16`. |
| `backend-deploy.yml` | Push a `main` sobre `backend/**` | Empaqueta el jar, construye la imagen `linux/arm64` con el `backend/Dockerfile`, la sube a ECR (`synhub-backend:latest` y `:<sha>`) y fuerza un redeploy en ECS si el servicio está activo. |

Requisitos en el repositorio de GitHub: variables `AWS_DEPLOY_ROLE_ARN` y `AWS_ACCOUNT_ID`, y un rol IAM con *trust policy* OIDC para `repo:Synhub-2-0/synhub_2:ref:refs/heads/main`.

> El `Dockerfile` de la raíz sirve para ejecutar la solución completa (local, demos o un único host). El despliegue en AWS sigue usando `backend/Dockerfile` (solo el jar) y S3/CloudFront para el frontend.
