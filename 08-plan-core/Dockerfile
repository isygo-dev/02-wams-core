FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl 04-plan-starter-ui -am package -DskipTests \
    && cp 04-plan-starter-ui/target/plan-management-starter-ui-*.jar /tmp/isygo-plan.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/isygo-plan.jar /app/isygo-plan.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/isygo-plan.jar"]
