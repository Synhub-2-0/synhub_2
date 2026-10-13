# Tour: backend

**No tiene backend.** El recorrido es contenido estático que se entrega con el frontend (`frontend/public/recorrido/synhub-recorrido.mp4` y `poster.jpg`).

En el despliegue con Docker, nginx sirve esos archivos desde `/usr/share/nginx/html/recorrido/`; en AWS los sirve el bucket S3 del frontend. Ver [Despliegue](../../../architecture/deployment.md).
