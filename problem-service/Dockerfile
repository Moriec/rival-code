FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY problem-service/pom.xml .
COPY problem-service/src ./src
COPY contracts/ ../contracts
COPY rivalcode-service-starter/ ../rivalcode-service-starter
RUN cd ../contracts && mvn clean install -DskipTests -q
RUN cd ../rivalcode-service-starter && mvn clean install -DskipTests -q
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]