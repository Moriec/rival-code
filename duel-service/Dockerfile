FROM maven:3.9.6-eclipse-temurin-21 AS app-builder
ARG MAVEN_CLI_OPTS="-B -ntp -DskipTests -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.rto=120000 -Daether.connector.connectTimeout=60000 -Daether.connector.requestTimeout=120000"

WORKDIR /app

COPY contracts ./contracts
COPY rivalcode-service-starter ./rivalcode-service-starter
COPY duel-service ./duel-service

RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd contracts && mvn ${MAVEN_CLI_OPTS} clean install
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd rivalcode-service-starter && mvn ${MAVEN_CLI_OPTS} clean install
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd duel-service && mvn ${MAVEN_CLI_OPTS} clean package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=app-builder /app/duel-service/target/duel-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
