# Build the Spring Boot executable JAR with the same Java version as the app.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

# Small Java runtime image for cloud deployment.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /workspace/target/officecentral.jar /app/officecentral.jar
USER 10001
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/officecentral.jar"]


