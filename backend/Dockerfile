# =============================================================================
# Backend Dockerfile - Multi-stage build
# =============================================================================

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache dependencies separately from source code
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B --no-transfer-progress

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests --no-transfer-progress

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Security: run as non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR from builder stage
COPY --from=builder /app/target/rag-platform-backend*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# JVM tuning for containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
