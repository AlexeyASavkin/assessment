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
RUN gradle bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
