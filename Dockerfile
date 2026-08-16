# Build stage
FROM gradle:9.6.1-jdk25-noble AS build
WORKDIR /app

# Copy Gradle files first for better layer caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradle.properties ./

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source and build
COPY src ./src
COPY api-specification ./api-specification
RUN gradle bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Install curl (for healthcheck) and create non-root user
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 app

USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
