# ---------------------------------------------------------------------------
# Multi-stage build for RoleBuilderAgent API
# ---------------------------------------------------------------------------

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first (layer caching)
COPY pom.xml .
COPY backend/agent-api/pom.xml backend/agent-api/pom.xml
RUN mvn -B dependency:go-offline -pl backend/agent-api -am

# Build the application
COPY backend/agent-api/src backend/agent-api/src
RUN mvn -B -pl backend/agent-api -am package -DskipTests \
    && mv backend/agent-api/target/agent-api-*.jar /app.jar

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
