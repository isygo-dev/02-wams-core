# Use OpenJDK 17 Alpine image for a lightweight base
FROM openjdk:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the service JAR file into the container
COPY 10-starter-test/40-document-management/04-dms-service/target/*.jar /app/service.jar

# Copy the necessary directories for uploads and camel
COPY 10-starter-test/40-document-management/04-dms-service/target/uploads /app/uploads
COPY 10-starter-test/40-document-management/04-dms-service/target/camel /app/camel

# Download and prepare the wait utility
RUN wget -q https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait -O /wait && \
    chmod +x /wait && \
    echo "Wait utility downloaded and made executable."

# Verify the contents of the uploads and camel directories for correctness
RUN echo "Listing uploads directory:" && ls -al /app/uploads && \
    echo "Listing camel directory:" && ls -al /app/camel

# Default command to wait for dependencies and start the application
CMD /wait && java -jar /app/service.jar
