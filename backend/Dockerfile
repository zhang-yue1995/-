FROM maven:3-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN groupadd --system xinsulu \
    && useradd --system --gid xinsulu --home-dir /app xinsulu \
    && mkdir -p /data/uploads \
    && chown -R xinsulu:xinsulu /app /data
COPY --from=build /workspace/target/xinsulu-backend-1.0.0.jar /app/app.jar
USER xinsulu
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
