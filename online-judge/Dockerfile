# --- Stage 1: Build Isolate ---
FROM ubuntu:22.04 AS isolate-builder
RUN apt-get update && apt-get install -y \
    build-essential \
    libcap-dev \
    libsystemd-dev \
    libseccomp-dev \
    git \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

RUN git clone https://github.com/ioi/isolate.git /tmp/isolate
WORKDIR /tmp/isolate
RUN make isolate isolate-cg-keeper default.cf && \
    install -s -m 755 isolate /usr/local/bin/ && \
    install -s -m 755 isolate-cg-keeper /usr/local/bin/ && \
    install -m 644 default.cf /usr/local/etc/isolate

# --- Stage 2: Build Contracts and Application ---
FROM maven:3.9.6-eclipse-temurin-21 AS app-builder
ARG MAVEN_CLI_OPTS="-B -ntp -DskipTests -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.rto=120000 -Daether.connector.connectTimeout=60000 -Daether.connector.requestTimeout=120000"
WORKDIR /build

COPY contracts ./contracts
COPY rivalcode-service-starter ./rivalcode-service-starter
COPY online-judge ./online-judge

RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd contracts && mvn ${MAVEN_CLI_OPTS} clean install
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd rivalcode-service-starter && mvn ${MAVEN_CLI_OPTS} clean install
RUN --mount=type=cache,id=rivalcode-maven-repo,target=/root/.m2,sharing=locked cd online-judge && mvn ${MAVEN_CLI_OPTS} clean package

# --- Stage 3: Final Runtime Image ---
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

RUN sed -i 's|http://|https://|g' /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    g++ \
    python3 \
    rustc \
    ruby \
    nodejs \
 && rm -rf /var/lib/apt/lists/*

COPY --from=isolate-builder /usr/local/bin/isolate /usr/local/bin/isolate
COPY --from=isolate-builder /usr/local/bin/isolate-cg-keeper /usr/local/bin/isolate-cg-keeper
COPY online-judge/isolate.conf /usr/local/etc/isolate

COPY --from=app-builder /build/online-judge/target/*.jar app.jar
COPY online-judge/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENV APP_SANDBOX_JAVA_PATH=/opt/java/openjdk/bin/java
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

ENTRYPOINT ["/app/entrypoint.sh"]
