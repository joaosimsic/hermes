FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY bom/pom.xml bom/
COPY events/pom.xml events/
COPY services/chat-service/pom.xml services/chat-service/

RUN --mount=type=cache,target=/root/.m2 \
    cd /app && mvn dependency:go-offline -pl services/chat-service -am -B

COPY bom bom
COPY events events
COPY services/chat-service services/chat-service

RUN --mount=type=cache,target=/root/.m2 \
    cd /app && mvn package -pl services/chat-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=build /app/services/chat-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
