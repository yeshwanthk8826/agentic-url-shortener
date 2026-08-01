# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress \
    -DskipTests \
    dependency:go-offline

COPY src ./src

RUN mvn --batch-mode --no-transfer-progress \
    -DskipTests \
    clean package


FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup --system app \
    && adduser --system --ingroup app app

WORKDIR /app

COPY --from=build \
    /workspace/target/agentic-url-shortener-*.jar \
    /app/application.jar

USER app

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="\
-XX:MaxRAMPercentage=75.0 \
-XX:+ExitOnOutOfMemoryError \
-Djava.security.egd=file:/dev/urandom"

ENTRYPOINT ["java", "-jar", "/app/application.jar"]