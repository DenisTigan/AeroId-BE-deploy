# Etapa 1: Construim aplicatia (Aici face el Maven Clean Package)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Rulam aplicatia
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expunem portul pe care ruleaza Spring Boot
EXPOSE 8080

# Comanda de start
ENTRYPOINT ["java", "-jar", "app.jar"]