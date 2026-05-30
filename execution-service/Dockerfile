FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package \
 && mv target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
