# 1. Use a base image with Java installed
FROM eclipse-temurin:21-jdk-alpine

# 2. Set the working directory inside the container
WORKDIR /app

# 3. Copy the JAR file from your build folder to the container
# For Maven: target/*.jar | For Gradle: build/libs/*.jar
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} poolerBackendApplication.jar

# 4. Expose the port your app runs on (default is 8080)
EXPOSE 8080

# 5. Define the command to run your app
ENTRYPOINT ["java", "-jar", "app.jar"]