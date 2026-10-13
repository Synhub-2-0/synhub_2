# SynHub 2.0

Monorepo de SynHub, que reúne:

| Carpeta      | Contenido                          | Repo original |
|--------------|------------------------------------|---------------|
| `frontend/`  | Web app (Angular)                  | [Synhub-Web-App-v2](https://github.com/Synhub-2-0/Synhub-Web-App-v2) |
| `backend/`   | API (Spring Boot, Java 21, Maven)  | [synhub_backend](https://github.com/Synhub-2-0/synhub_backend) |

El historial de commits de ambos repos se conservó al migrar; los archivos de cada uno viven en su carpeta.

## Desarrollo

```bash
# Frontend
cd frontend && npm ci && npx ng serve

# Backend
cd backend && ./mvnw spring-boot:run
```

## CI/CD

Los workflows están en `.github/workflows/` y solo se disparan cuando cambian archivos de su carpeta:

- `frontend-deploy.yml`: tests + build del frontend; en `main` despliega a S3/CloudFront.
- `backend-ci.yml`: `mvnw verify` con PostgreSQL.
- `backend-deploy.yml`: en `main` construye la imagen, la sube a ECR y redepliega en ECS.
