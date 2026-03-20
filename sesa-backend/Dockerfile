# Imagen de producción para Render (Java 17, usuario no root).
# Autor: Ing. J Sebastian Vargas S
# Build (desde sesa-backend): docker build -t sesa-api .
# Render: Root Directory = sesa-backend, Dockerfile path = Dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system sesa && useradd --system --gid sesa sesa

COPY --from=build /app/target/sesa-backend-*.jar /app/app.jar
RUN chown sesa:sesa /app/app.jar

USER sesa

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Render inyecta PORT; Spring lo lee desde application-prod.yml
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
