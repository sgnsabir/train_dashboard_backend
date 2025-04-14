# Stage 1: Build the application using Maven with Java 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy the pom.xml first to download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application (skip tests)
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application using a lightweight JDK image for Java 21
# Using the Debian‑based image (instead of Alpine) for better DNS resolution and overall production reliability
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the built jar from the builder stage (adjust jar name if needed)
COPY --from=builder /app/target/predictive-maintenance-backend-*.jar app.jar

# Expose the port the Spring Boot application listens on
EXPOSE 8080

# Run the application with optimal container settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=80", "-jar", "app.jar"]
