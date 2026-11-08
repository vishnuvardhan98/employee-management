# syntax=docker/dockerfile:1
#
# One image for the whole application: the Angular build is bundled into the Spring Boot jar
# and served from the same origin as the REST API (http://localhost:8080).
# MySQL runs as a separate container — see docker-compose.yml.

# ---------- 1. Build the Angular frontend ----------
FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---------- 2. Build (and test) the Spring Boot backend ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/employee-management/browser ./src/main/resources/static
# Unit + integration tests run on an in-memory H2 database; pass --build-arg SKIP_TESTS=true to skip.
ARG SKIP_TESTS=false
RUN mvn -B -q package -DskipTests=${SKIP_TESTS}

# ---------- 3. Minimal runtime image ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend /build/backend/target/peopledesk.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
