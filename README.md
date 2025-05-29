# WebFlux Reactive Backend

A high-performance, reactive Spring Boot 3 application exposing R2DBC-based CRUD, filtering, and predictive‐maintenance endpoints. Secured with JWT, it integrates with our AI microservice and supports real‐time alerting.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Prerequisites](#prerequisites)
4. [Getting Started](#getting-started)

    1. [Clone the Repository](#clone-the-repository)
    2. [Configuration](#configuration)
    3. [Docker Compose](#docker-compose)
    4. [Build & Run](#build--run)
5. [API Documentation](#api-documentation)
6. [Security & Authentication](#security--authentication)
7. [Database Migrations](#database-migrations)
8. [Monitoring & Metrics](#monitoring--metrics)
9. [Development Tips](#development-tips)
10. [Troubleshooting](#troubleshooting)

---

## Features

* **Reactive REST API** using Spring WebFlux
* **R2DBC Postgres** repositories for non‐blocking I/O
* **Dynamic filtering & pagination** across domain entities
* **Predictive Maintenance** endpoints integrating with AI microservice
* **JWT Authorization** with role‐based access
* **Alert History** for real‐time operator notifications
* **Swagger / OpenAPI** for self‐documenting API
* **Micrometer + Prometheus** metrics support

---

## Tech Stack

* Java 17+
* Spring Boot 3 (WebFlux, Security, R2DBC, Data)
* PostgreSQL (R2DBC)
* JWT (io.jsonwebtoken)
* Project Reactor (Flux, Mono)
* Lombok
* Swagger/OpenAPI (springdoc-openapi)
* Micrometer + Prometheus
* Docker / Docker Compose

---

## Prerequisites

* **Java 17** or higher
* **Maven 3.8+** or **Gradle 7+**
* **Docker** & **Docker Compose**
* **Git**

---

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/your-org/rail-ai-platform.git
cd rail-ai-platform/apps/webflux-backend
```

### Configuration

1. Copy the sample environment file:

   ```bash
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   ```
2. **`application.yml`** highlights the key settings:

   ```yaml
   spring:
     r2dbc:
       url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:rail_db}
       username: ${DB_USER:postgres}
       password: ${DB_PASS:postgres}
   maintenance:
     speed-threshold: 80.0
     aoa-threshold: 5.0
   jwt:
     secret: ${JWT_SECRET:base64-encoded-512bit-key}
     expirationSeconds: 3600
   ```
3. Create a **`.env`** file in the same folder (if you prefer Docker Compose env injection):

   ```dotenv
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=rail_db
   DB_USER=postgres
   DB_PASS=postgres
   JWT_SECRET=c2VjdXJlSldUU2VjcmV0S2V5MTIz… (at least 64 bytes base64)
   AI_SERVICE_URL=http://ai-service:8000
   ```

### Docker Compose

A provided `docker-compose.yml` will spin up:

* **postgres**: Reactive Postgres for R2DBC
* **webflux-backend**: Your Spring Boot app

```bash
# From rail-ai-platform/apps/webflux-backend
docker-compose up -d
```

**docker-compose.yml** snippet:

```yaml
version: "3.8"
services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: ${DB_NAME:-rail_db}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASS:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data

  webflux-backend:
    build: .
    env_file: .env
    ports:
      - "8080:8080"
    depends_on:
      - db

volumes:
  db_data:
```

### Build & Run

#### 1. Build the JAR

```bash
mvn clean package -DskipTests
```

#### 2. Run Locally (without Docker)

```bash
java -jar target/webflux-backend-0.1.0.jar
```

#### 3. Run via Docker Compose

```bash
docker-compose up -d --build
```

Verify logs:

```bash
docker-compose logs -f webflux-backend
```

---

## API Documentation

Once the app is running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

* **Health check**: `GET  /health`
* **Predict Maintenance**:
  `GET  /api/v1/predictive/{analysisId}?alertEmail={email}`
* **Get Schedule**:
  `GET  /api/v1/maintenance/schedule`
* **Alert History** CRUD & filters\*\*:
  `GET  /api/v1/alerts?acknowledged=true&page=0&size=20`

All endpoints require a valid `Bearer <JWT>` token with role `MAINTENANCE`.

---

## Security & Authentication

1. **Register** or **Login** via your AI microservice or identity provider
2. Include the returned **JWT** in `Authorization` header:

   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```
3. Tokens are validated against `jwt.secret` and expiration.

---

## Database Migrations

This project assumes the following tables exist in Postgres:

* `alert_history`
* `digital_twins`
* `haugfjell_mp1_axles`, `haugfjell_mp1_header`
* `haugfjell_mp3_axles`, `haugfjell_mp3_header`
* `train_health`
* `users`, `user_settings`, `user_dashboard_settings`

You can initialize via your preferred tool (Flyway, Liquibase) or run the provided SQL DDL located in `infra/db/migrations/*.sql`.

---

## Monitoring & Metrics

* **Prometheus** scraping endpoint:

  ```
  http://localhost:8080/actuator/prometheus
  ```
* **Micrometer** counters for repository calls, scheduler, and security events.
* **Log** structured with MDC (request IDs, timestamps).

---

## Development Tips

* **Hot reload**: Use DevTools for live reload on code changes.
* **IDE support**: Lombok, Spring Boot Run configurations.
* **Reactive debugging**: `.log()` on Reactor chains, WebTestClient for integration tests.

---

## Troubleshooting

* **DB Connectivity**: Ensure Docker’s Postgres is up (`docker ps`) and `.env` matches ports.
* **JWT errors**: Check `JWT_SECRET` is a valid 64-byte base64 key.
* **Port clashes**: Adjust `server.port` in `application.yml`.
* **Missing tables**: Run SQL DDL or attach your migration tool.

## **Happy coding!** 🚀

**License:** Apache 2.0
