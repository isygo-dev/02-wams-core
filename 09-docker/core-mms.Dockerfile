# Use OpenJDK 17 Alpine image as a lightweight base
FROM openjdk:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the service JAR file
COPY 10-starter-test/30-messaging-management/04-mms-service/target/*.jar /app/service.jar

# Copy the necessary directories (uploads, camel, msgtemplate) into the container
COPY 10-starter-test/30-messaging-management/04-mms-service/target/uploads /app/uploads
COPY 10-starter-test/30-messaging-management/04-mms-service/target/camel /app/camel
COPY 10-starter-test/30-messaging-management/04-mms-service/target/msgtemplate /app/msgtemplate

# Download and prepare the wait utility to ensure dependencies are ready
RUN wget -q https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait -O /wait && \
    chmod +x /wait && \
    echo "Wait utility downloaded and set executable."

# List the contents of the directories to verify they were copied correctly
RUN echo "Listing uploads directory:" && ls -al /app/uploads && \
    echo "Listing camel directory:" && ls -al /app/camel && \
    echo "Listing msgtemplate directory:" && ls -al /app/msgtemplate

# Default command to run the wait utility and start the Spring Boot app
CMD /wait && java -jar /app/service.jar