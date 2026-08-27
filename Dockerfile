# Multi-stage build for Spring Boot application
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

# Copy source code
COPY src src

# Build application
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=30s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
