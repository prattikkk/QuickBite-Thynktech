# QuickBite 🍔

A modern food delivery marketplace connecting customers, vendors, and drivers.

## 🎯 Overview

QuickBite is a full-stack food delivery platform built with:
- **Backend**: Spring Boot (Java 17), PostgreSQL, Redis
- **Frontend**: React, TypeScript, Vite
- **Deployment**: Docker, Docker Compose

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 17+ (for backend development)
- Node.js 18+ (for frontend development)
- Maven 3.8+

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/quickbite.git
cd quickbite
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL and Redis
docker-compose up -d

# Verify services are running
docker-compose ps
```

This starts:
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`
- PgAdmin on `localhost:8081` (optional)

### 3. Start Backend

```bash
cd backend

# Set environment variables (or create .env file)
export DB_HOST=localhost
export DB_USERNAME=dbuser
export DB_PASSWORD=dbpass
export JWT_SECRET=your-secret-key-min-256-bits

# Run with Maven
mvn spring-boot:run
```

Backend will be available at: http://localhost:8080

API Documentation (Swagger): http://localhost:8080/swagger-ui.html

Health Check: http://localhost:8080/api/health

### 4. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will be available at: http://localhost:5173

### 5. Verify Setup

```bash
# Test backend health
curl http://localhost:8080/api/health

# Should return:
# {"status":"UP","service":"QuickBite API","timestamp":...}
```

## 📁 Project Structure

```
QuickBite/
├── backend/                  # Spring Boot application
│   ├── src/main/java/
│   │   └── com/quickbite/
│   │       ├── auth/        # Authentication & JWT
│   │       ├── users/       # User management
│   │       ├── vendors/     # Vendor management
│   │       ├── menu/        # Menu items
│   │       ├── orders/      # Order lifecycle
│   │       ├── payments/    # Payment processing
│   │       ├── drivers/     # Driver management
│   │       ├── admin/       # Admin dashboard
│   │       └── common/      # Shared utilities
│   ├── src/main/resources/
│   │   └── db/migration/    # Flyway migrations
│   └── pom.xml
├── frontend/                 # React application
│   ├── src/
│   │   ├── pages/           # Route components
│   │   ├── components/      # Reusable components
│   │   ├── services/        # API integration
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.ts
├── docs/                     # Documentation
│   ├── architecture.md
│   ├── functional_requirements.md
│   ├── non_functional_requirements.md
│   └── erd.mmd
├── docker-compose.yml
└── README.md
```

## 🏗️ Architecture

QuickBite uses a **modular monolith** architecture for MVP, allowing:
- Fast development and iteration
- ACID transactions across modules
- Simple deployment (single artifact)
- Clear module boundaries for future extraction

### Core User Flows

1. **Customer Journey**
   - Browse vendors by location
   - View menus and add items to cart
   - Place order with payment
   - Track order in real-time
   - Receive delivery

2. **Vendor Journey**
   - Register and set up profile
   - Manage menu items
   - Receive and accept orders
   - Update order status
   - View analytics

3. **Driver Journey**
   - View available delivery jobs
   - Accept delivery
   - Navigate to pickup and drop-off
   - Update GPS location
   - Complete delivery

See [docs/architecture.md](docs/architecture.md) for detailed architecture documentation.

## 🗃️ Database

### Migrations

Database schema is managed by Flyway. Migrations run automatically on application startup.

```bash
# View migration status
cd backend
mvn flyway:info

# Manually run migrations (if needed)
mvn flyway:migrate
```

### Access Database

Using PgAdmin (http://localhost:8081):
- Email: `admin@quickbite.com`
- Password: `admin`
- Server: `postgres`
- Database: `quickbite`
- Username: `dbuser`
- Password: `dbpass`

Or using psql:
```bash
psql -h localhost -U dbuser -d quickbite
```

## 🔐 Environment Variables

### Backend

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | PostgreSQL host | `localhost` | Yes |
| `DB_PORT` | PostgreSQL port | `5432` | No |
| `DB_NAME` | Database name | `quickbite` | Yes |
| `DB_USERNAME` | Database user | `dbuser` | Yes |
| `DB_PASSWORD` | Database password | `dbpass` | Yes |
| `JWT_SECRET` | JWT signing key | - | Yes |
| `CORS_ORIGINS` | Allowed CORS origins | `http://localhost:5173` | No |

### Frontend

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API URL | `/api` (proxied) |

## 🧪 Testing

### Backend Tests

```bash
cd backend

# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Frontend Tests

```bash
cd frontend

# Run tests (to be implemented)
npm test
```

## 📦 Building for Production

### Backend

```bash
cd backend
mvn clean package -DskipTests

# JAR file location
ls target/quickbite-backend-*.jar
```

### Frontend

```bash
cd frontend
npm run build

# Built files in dist/
ls dist/
```

## 🐳 Docker Deployment

```bash
# Build and run all services
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Remove volumes (WARNING: deletes data)
docker-compose down -v
```

## 📚 API Documentation

Interactive API documentation is available via Swagger UI:

- **Development**: http://localhost:8080/swagger-ui.html
- **API Spec**: http://localhost:8080/api-docs

### Sample API Endpoints

```bash
# Health check
GET /api/health

# Authentication
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh

# Vendors
GET  /api/vendors?lat=40.7128&lng=-74.0060&radius=5
GET  /api/vendors/{id}
POST /api/vendors
GET  /api/vendors/{id}/menu

# Orders
POST /api/orders
GET  /api/orders/{id}
GET  /api/orders/my-orders
PATCH /api/orders/{id}/status

# Payments
POST /api/payments/intent
POST /api/payments/confirm
```

## 🛠️ Development Workflow

### Branch Strategy

- `main` - Production-ready code
- `develop` - Integration branch
- `feature/*` - Feature branches
- `bugfix/*` - Bug fix branches

### Making Changes

1. Create feature branch from `develop`
   ```bash
   git checkout develop
   git pull
   git checkout -b feature/your-feature-name
   ```

2. Make changes and commit
   ```bash
   git add .
   git commit -m "feat: add vendor search by cuisine"
   ```

3. Push and create PR
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create PR against `develop` branch

See [.github/PULL_REQUEST_TEMPLATE.md](.github/PULL_REQUEST_TEMPLATE.md) for PR guidelines.

## 🐛 Troubleshooting

### Backend won't start

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check backend logs
cd backend
mvn spring-boot:run

# Verify database connection
psql -h localhost -U dbuser -d quickbite -c "SELECT 1;"
```

### Frontend can't connect to backend

```bash
# Verify backend is running
curl http://localhost:8080/api/health

# Check Vite proxy configuration
cat frontend/vite.config.ts

# Clear browser cache and restart
```

### Database migration errors

```bash
# Check migration status
cd backend
mvn flyway:info

# Repair Flyway metadata (use with caution)
mvn flyway:repair
```

### Port conflicts

```bash
# Check what's using port 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Mac/Linux

# Change backend port
SERVER_PORT=8081 mvn spring-boot:run

# Change frontend port
npm run dev -- --port 3000
```

## 📖 Documentation

- [Architecture Overview](docs/architecture.md)
- [Functional Requirements](docs/functional_requirements.md)
- [Non-Functional Requirements](docs/non_functional_requirements.md)
- [Entity Relationship Diagram](docs/erd.mmd)
- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)

## 🚦 Project Status

**Current Phase**: Day 1 - Setup Complete ✅

See [DAY1_CHECKLIST.md](DAY1_CHECKLIST.md) for setup verification.

### Roadmap

- [x] Day 1: Architecture & Scaffolding
- [ ] Day 2: Core Entities & Repositories
- [ ] Day 3: Authentication & Authorization
- [ ] Day 4: Business Logic & Services
- [ ] Day 5: Frontend Integration
- [ ] Day 6: Testing & Documentation
- [ ] Day 7: Deployment & Launch

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

See [Contributing Guidelines](CONTRIBUTING.md) for details.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Backend Team**: Spring Boot development
- **Frontend Team**: React development
- **DevOps Team**: Infrastructure & deployment
- **Product Team**: Requirements & design

## 📧 Contact

For questions or support:
- Create an issue on GitHub
- Email: support@quickbite.com
- Slack: #quickbite-dev

---

**Made with ❤️ by the QuickBite Team**
