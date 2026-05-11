# Stage 1: Build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cache Gradle wrapper
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime (Debian-based, glibc — not Alpine/musl)
FROM eclipse-temurin:25-jre
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
