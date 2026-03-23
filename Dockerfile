FROM gradle:8.14.1-jdk17-alpine AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

