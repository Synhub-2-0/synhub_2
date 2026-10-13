# Imagen única con toda la solución: nginx sirve el frontend en :80 y
# reenvía /api/ al backend Spring Boot, que corre en el mismo contenedor (:8080).

# ---- Frontend (Angular) ----
FROM node:22-alpine AS frontend
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build

# ---- Backend (Spring Boot) ----
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /src
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY backend/src src
# Los tests necesitan PostgreSQL; se ejecutan en CI, no en el build de la imagen.
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar /app.jar

# ---- Runtime ----
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends nginx curl \
 && rm -rf /var/lib/apt/lists/* /etc/nginx/sites-enabled/default \
 && useradd --system --uid 10001 app
COPY docker/nginx.conf /etc/nginx/conf.d/synhub.conf
COPY docker/entrypoint.sh /entrypoint.sh
COPY --from=frontend /src/dist/Synhub-website/browser /usr/share/nginx/html
COPY --from=backend /app.jar /app/app.jar
RUN chmod +x /entrypoint.sh
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://127.0.0.1/actuator/health || exit 1
ENTRYPOINT ["/entrypoint.sh"]
