# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --uid 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

ENV SERVER_PORT=8080
EXPOSE 8080

CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-$SERVER_PORT}"]
