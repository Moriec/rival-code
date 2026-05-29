# Multi-stage build for test-online-judge

FROM maven:3.9.6-eclipse-temurin-21 as app-builder
ARG MAVEN_CLI_OPTS="-B -ntp -DskipTests -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.rto=120000 -Daether.connector.connectTimeout=60000 -Daether.connector.requestTimeout=120000"
WORKDIR /build

# Copy contracts module
COPY contracts ./contracts
# Copy test-online-judge module
COPY test-online-judge ./test-online-judge

# Build contracts first
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd contracts && mvn ${MAVEN_CLI_OPTS} clean install

# Build test-online-judge
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd test-online-judge && mvn ${MAVEN_CLI_OPTS} clean package

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

COPY --from=app-builder /build/test-online-judge/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
