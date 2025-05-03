# ─── Stage 1: build ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# compile & package (skip tests)
COPY src ./src
RUN mvn clean package -DskipTests

# ─── Stage 2: runtime ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# copy the built jar
COPY --from=builder /app/target/predictive-maintenance-backend-*.jar app.jar

# set a reasonable default profile; override with SPRING_PROFILES_ACTIVE
ENV SPRING_PROFILES_ACTIVE=development \
    SPRING_ZIPKIN_ENABLED=false

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=80","-jar","/app/app.jar"]
