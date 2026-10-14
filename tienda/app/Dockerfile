# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# The Java tracer. One jar, no code changes: that is the whole point of APM auto-instrumentation.
ADD https://dtdg.co/latest-java-tracer /app/dd-java-agent.jar
COPY --from=build /src/target/shop-api.jar /app/shop-api.jar
EXPOSE 8081
# Everything else (service, env, version, agent host...) comes from DD_* environment variables.
ENTRYPOINT ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "/app/shop-api.jar"]
