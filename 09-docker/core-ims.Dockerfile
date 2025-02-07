# Use OpenJDK 17 Alpine image for a lightweight base
FROM openjdk:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file of the service
COPY 10-starter-test/20-identity-management/04-ims-service/target/*.jar /app/service.jar

# Copy the necessary directories (uploads and camel) into the container
COPY 10-starter-test/20-identity-management/04-ims-service/target/uploads /app/uploads
COPY 10-starter-test/20-identity-management/04-ims-service/target/camel /app/camel

# Download and prepare the wait utility
RUN wget -q https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait -O /wait && \
    chmod +x /wait && \
    echo "Wait utility downloaded and set executable."

# List the contents of the uploads and camel directories to verify the copy
RUN echo "Listing uploads directory:" && ls -al /app/uploads && \
    echo "Listing camel directory:" && ls -al /app/camel

# Default command to run the wait utility and then the Spring Boot app
CMD /wait && java -jar /app/service.jar
