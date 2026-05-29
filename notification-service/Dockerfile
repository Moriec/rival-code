FROM maven:3.9.6-eclipse-temurin-21 AS build
ARG MAVEN_CLI_OPTS="-B -ntp -DskipTests -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.rto=120000 -Daether.connector.connectTimeout=60000 -Daether.connector.requestTimeout=120000"
WORKDIR /build
COPY notification-service/pom.xml .
COPY notification-service/src ./src
COPY contracts/ ../contracts
COPY rivalcode-service-starter/ ../rivalcode-service-starter
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd ../contracts && mvn ${MAVEN_CLI_OPTS} clean install
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd ../rivalcode-service-starter && mvn ${MAVEN_CLI_OPTS} clean install
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked mvn ${MAVEN_CLI_OPTS} clean package

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
