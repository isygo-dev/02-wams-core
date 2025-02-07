# Use OpenJDK 17 Alpine image for a lightweight base
FROM openjdk:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file of the service into the container
COPY 10-starter-test/10-key-management/04-kms-service/target/*.jar /app/service.jar

# Copy the necessary directories (uploads and camel) into the container
COPY 10-starter-test/10-key-management/04-kms-service/target/uploads /app/uploads
COPY 10-starter-test/10-key-management/04-kms-service/target/camel /app/camel

# Download and prepare the wait utility for dependency checks
RUN wget -q https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait -O /wait && \
    chmod +x /wait && \
    echo "Wait utility downloaded and set executable."

# List the contents of the uploads and camel directories to verify the files
RUN echo "Listing uploads directory:" && ls -al /app/uploads && \
    echo "Listing camel directory:" && ls -al /app/camel

# Default command to run the wait utility and then the Spring Boot application
CMD /wait && java -jar /app/service.jar