FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache curl

# Copy built JAR and additional folders from the builder stage
COPY 10-starter-test/30-messaging-management/04-mms-service/target/*.jar service.jar
COPY 10-starter-test/30-messaging-management/04-mms-service/target/uploads /uploads
COPY 10-starter-test/30-messaging-management/04-mms-service/target/camel /camel
COPY 10-starter-test/30-messaging-management/04-mms-service/target/msgtemplate /msgtemplate

# Download docker-compose-wait
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

EXPOSE 40404

CMD /wait && java -jar service.jar
