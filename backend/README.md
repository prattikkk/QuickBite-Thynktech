# QuickBite Backend

Spring Boot backend for the QuickBite food delivery marketplace.

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 13+
- Docker (optional, for local database)

## Quick Start

### 1. Start PostgreSQL

Using Docker Compose (from project root):
```bash
docker-compose up -d postgres
```

Or use an existing PostgreSQL instance and create a database:
```sql
CREATE DATABASE quickbite;
```

### 2. Configure Environment Variables

Create a `.env` file in the backend directory or export variables:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=quickbite
export DB_USERNAME=dbuser
export DB_PASSWORD=dbpass
export JWT_SECRET=your-secret-key-min-256-bits
```

### 3. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run with environment variables inline
DB_HOST=localhost DB_USERNAME=dbuser DB_PASSWORD=dbpass mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Verify Health

```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "QuickBite API",
  "timestamp": 1708329600000
}
```

## API Documentation

Once running, access Swagger UI at:
- http://localhost:8080/swagger-ui.html

OpenAPI JSON:
- http://localhost:8080/api-docs

## Project Structure

```
com.quickbite/
├─ config/           # Spring configuration classes
├─ auth/             # Authentication & JWT
├─ users/            # User management
│  ├─ entity/        # User, Role entities
│  ├─ repository/    # JPA repositories
│  ├─ service/       # Business logic
│  └─ controller/    # REST controllers
├─ vendors/          # Vendor management
├─ menu/             # Menu items
├─ orders/           # Order lifecycle
├─ payments/         # Payment processing
├─ drivers/          # Driver management
├─ admin/            # Admin dashboard
└─ common/           # Shared utilities
   ├─ dto/           # Data Transfer Objects
   ├─ exceptions/    # Custom exceptions
   └─ utils/         # Helper classes
```

## Database Migrations

Flyway automatically runs migrations on startup. Migration files are in:
```
src/main/resources/db/migration/
```

To see migration status:
```bash
mvn flyway:info
```

## Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Building for Production

```bash
# Create executable JAR
mvn clean package -DskipTests

# Run the JAR
java -jar target/quickbite-backend-0.0.1-SNAPSHOT.jar
```

## Environment Variables Reference

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `quickbite` |
| `DB_USERNAME` | Database username | `dbuser` |
| `DB_PASSWORD` | Database password | `dbpass` |
| `JWT_SECRET` | JWT signing key (256+ bits) | *(required in production)* |
| `SHOW_SQL` | Show SQL queries in logs | `false` |
| `CORS_ORIGINS` | Allowed CORS origins | `http://localhost:5173` |

## Actuator Endpoints

Monitor application health and metrics:

- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/actuator/metrics` - Metrics
- `/actuator/prometheus` - Prometheus metrics

## Troubleshooting

### Connection refused to PostgreSQL
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check connection
psql -h localhost -U dbuser -d quickbite
```

### Flyway migration errors
```bash
# Repair Flyway metadata (use with caution)
mvn flyway:repair

# Clean database (WARNING: drops all data)
mvn flyway:clean
```

### Port 8080 already in use
```bash
# Change port via environment variable
SERVER_PORT=8081 mvn spring-boot:run
```

## Next Steps

- [ ] Implement JWT authentication (Day 3)
- [ ] Add remaining entities (vendors, orders, etc.)
- [ ] Create service layer with business logic
- [ ] Add integration tests
- [ ] Configure Redis for caching
- [ ] Add API rate limiting

## License

See LICENSE file in project root.
