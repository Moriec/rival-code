FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY auth-service/pom.xml .
COPY auth-service/src ./src
COPY contracts/ ../contracts
RUN cd ../contracts && mvn clean install -DskipTests -q
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]