# SynHub: frontend

Aplicación web en Angular 22 (signals, Tailwind y Angular Material). Es parte del monorepo: la visión general está en el [README principal](../README.md).

## Comandos

```bash
npm ci
npx ng serve                 # http://localhost:4200, contra el backend en http://localhost:8080 (perfil dev)
npx ng test --watch=false    # Vitest
npx ng build                 # dist/Synhub-website/browser
```

## Documentación

- Arquitectura y convenciones del frontend: [docs/architecture/frontend.md](../docs/architecture/frontend.md).
- Cada feature (iam, groups, invitations, tasks, requests, ai, tour) tiene su parte de frontend en `docs/features/<feature>/frontend/README.md`. El índice está en [docs/README.md](../docs/README.md).
- Pruebas y su estado: [TESTS.md](../TESTS.md).
