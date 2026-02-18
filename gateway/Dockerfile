FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY bom/pom.xml bom/
RUN --mount=type=cache,target=/root/.m2 \
    mvn install -f bom/pom.xml -DskipTests -q

COPY gateway/pom.xml gateway/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f gateway/pom.xml dependency:go-offline -q

COPY gateway/src gateway/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f gateway/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser

COPY --from=builder /app/gateway/target/*.jar app.jar

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
