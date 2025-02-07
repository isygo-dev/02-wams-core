# Use the official OpenJDK 17 Alpine image as the base image
FROM openjdk:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the application JAR file
COPY 10-starter-test/60-calendar-management/04-cms-service/target/*.jar /app/service.jar

# Copy the necessary directories
COPY 10-starter-test/60-calendar-management/04-cms-service/target/uploads /app/uploads
COPY 10-starter-test/60-calendar-management/04-cms-service/target/camel /app/camel

# Download the wait utility and make it executable
RUN wget -q https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait -O /wait && \
    chmod +x /wait && \
    echo "Wait utility downloaded and permissions set."

# List the contents of the uploads and camel directories for verification
RUN echo "Listing uploads directory:" && ls -al /app/uploads && \
    echo "Listing camel directory:" && ls -al /app/camel

# Remove unnecessary files after copying (if needed) to reduce the image size
# This step is optional based on whether you want to keep the files or not
# RUN rm -rf /app/uploads/* /app/camel/*

# Define the default command to run the Spring Boot application with wait utility
CMD /wait && java -jar /app/service.jar