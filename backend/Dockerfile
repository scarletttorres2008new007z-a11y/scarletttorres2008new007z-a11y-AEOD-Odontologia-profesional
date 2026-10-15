# Servicio «backend» de Railway (en Railway: Settings → Root Directory = /backend).
# 1) Compila el .jar con Maven. Las pruebas ya pasan en GitHub (CI) con MySQL y MariaDB.
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -B package -DskipTests && cp target/*.jar /app/clinica-api.jar

# 2) Solo Java 21 y el .jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/clinica-api.jar ./clinica-api.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Europe/Madrid"
EXPOSE 8080
USER 1000
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/clinica-api.jar"]
