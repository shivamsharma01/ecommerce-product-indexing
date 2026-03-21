# syntax=docker/dockerfile:1
# Multi-stage: layered Spring Boot JAR for better layer cache + small JRE runtime.

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

RUN apk add --no-cache bash

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test \
    && JAR_FILE=$(ls build/libs/*.jar | grep -v plain | head -n1) \
    && mkdir -p /app/layers \
    && cd /app/layers \
    && java -Djarmode=layertools -jar "$JAR_FILE" extract

FROM eclipse-temurin:17-jre-alpine AS runtime

RUN apk add --no-cache dumb-init tzdata \
    && addgroup -g 1000 app \
    && adduser -D -u 1000 -G app app

WORKDIR /app

ENV TZ=UTC \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

COPY --from=builder --chown=app:app /app/layers/dependencies/ ./
COPY --from=builder --chown=app:app /app/layers/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /app/layers/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /app/layers/application/ ./

USER app:app
EXPOSE 8085

ENTRYPOINT ["/usr/bin/dumb-init", "--", "java", "org.springframework.boot.loader.launch.JarLauncher"]
