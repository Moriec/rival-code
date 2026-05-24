# Multi-stage build for test-online-judge

FROM maven:3.9.6-eclipse-temurin-21 as app-builder
WORKDIR /build

# Copy contracts module
COPY contracts ./contracts
# Copy test-online-judge module
COPY test-online-judge ./test-online-judge

# Build contracts first
RUN cd contracts && mvn clean install -DskipTests -q

# Build test-online-judge
RUN cd test-online-judge && mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

COPY --from=app-builder /build/test-online-judge/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
