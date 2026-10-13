# Tour: recorrido guiado

Página con un video que muestra todas las funciones de SynHub, dividido en capítulos (inicio de sesión, panel, grupos, tareas, IA, validaciones…) para saltar directo a cada parte.

| | |
|---|---|
| **Recurso REST** | Ninguno |
| **Bounded context (backend)** | Ninguno |
| **Módulo (frontend)** | [`src/app/tour`](../../../frontend/src/app/tour) |
| **Ruta de la web** | `/recorrido` (con `iamGuard`) |
| **Documentación** | [Backend](backend/README.md) · [Frontend](frontend/README.md) |

## Comunicación frontend ↔ backend

Ninguna. El video y el póster son archivos estáticos de `frontend/public/recorrido/` que se sirven junto con la web (nginx en el contenedor, o S3/CloudFront en AWS).

## Propiedad

Es una feature **solo de frontend**. Los capítulos describen pantallas de las demás features: si una de esas pantallas cambia mucho, hay que volver a grabar el video y actualizar los tiempos en `tour-chapter.ts`.
