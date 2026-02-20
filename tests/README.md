# QuickBite E2E Test Suite

Comprehensive end-to-end testing suite for the QuickBite food delivery platform. Tests cover API endpoints, user journeys, WebSocket real-time updates, payment webhooks, and security validations.

## 📋 Table of Contents

- [Overview](#overview)
- [Test Coverage](#test-coverage)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Running Tests Locally](#running-tests-locally)
- [Test Components](#test-components)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

## 🎯 Overview

The E2E test suite validates the complete QuickBite system including:

- **REST API**: All endpoints with authentication, authorization, and data validation
- **User Journeys**: Customer, vendor, and driver workflows from end to end
- **WebSocket**: Real-time order status updates via STOMP
- **Webhooks**: Payment provider webhook simulation with HMAC-SHA256 signature verification
- **Security**: JWT authentication, role-based access control, input validation
- **Idempotency**: Duplicate event handling and eventual consistency

## 🧪 Test Coverage

### 1. Postman/Newman Collection
- **Location**: `tests/postman/quickbite-e2e.postman_collection.json`
- **Endpoints Covered**: 30+ API endpoints
- **Test Assertions**: 100+ automated checks

### 2. Cypress E2E Tests
- **Location**: `frontend/cypress/e2e/quickbite_e2e.cy.ts`
- **User Journeys**:
  - Customer registration, login, browse vendors, create order, payment flow
  - Vendor receives order, accepts, prepares, marks ready
  - Driver gets assigned, picks up, delivers
- **Edge Cases**: Invalid data, unauthorized access, non-existent resources

### 3. WebSocket Tests
- **Location**: `tests/ws/order_updates.test.js`
- **Framework**: Jest with @stomp/stompjs
- **Tests**: Real-time order status broadcasts, subscription management, connection resilience

### 4. Webhook Simulator
- **Location**: `tests/utils/send_webhook.sh`
- **Features**: HMAC-SHA256 signature generation (hex & base64), timestamp handling

## 📦 Prerequisites

### Required Software

- **Node.js**: v20+ (for Newman, Cypress, Jest)
- **Java**: JDK 17+ (for backend)
- **PostgreSQL**: 13+ (for database)
- **Redis**: 6+ (for sessions/WebSocket)
- **curl**: For health checks
- **jq**: For JSON processing (optional but recommended)
- **openssl**: For webhook signature generation

### Environment Setup

Ensure backend and frontend are running:

```bash
# Backend (Spring Boot)
cd backend
mvn clean package
java -jar target/*.jar

# Frontend (Vite + React)
cd frontend
npm install
npm run dev

# Or use Docker Compose
docker compose -f docker-compose.prod.yml up -d
```

## 🚀 Setup

### 1. Install Test Dependencies

```bash
# Install Newman (Postman CLI)
npm install -g newman newman-reporter-htmlextra

# Install Cypress dependencies (from frontend directory)
cd frontend
npm install --save-dev cypress

# Install WebSocket test dependencies (from tests/ws directory)
cd tests/ws
npm init -y
npm install --save-dev jest @stomp/stompjs ws @types/jest @types/node
```

### 2. Seed Test Database

Run the test seed script to create known test users:

```bash
# Via psql
psql -h localhost -U quickbite -d quickbite -f tests/seed/test_seed.sql

# Or via Docker
docker exec -i quickbite-postgres-1 psql -U quickbite -d quickbite < tests/seed/test_seed.sql
```

This creates:
- **Test Customer**: `e2e_test_customer` / `Test@1234`
- **Test Vendor**: `e2e_test_vendor` / `Test@1234`
- **Test Driver**: `e2e_test_driver` / `Test@1234`
- **Test Admin**: `e2e_test_admin` / `Test@1234`
- **Test Vendor Entity**: "E2E Test Restaurant" with 3 menu items

### 3. Set Environment Variables

Create `.env.test` file:

```bash
# API Configuration
API_BASE_URL=http://localhost:8080
FRONTEND_BASE_URL=http://localhost:5173
WS_URL=ws://localhost:8080/ws

# Security
JWT_SECRET=a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2
PAYMENT_WEBHOOK_SECRET=whsec_test_placeholder_secret_2026

# Database (if running tests against separate DB)
DB_USERNAME=quickbite
DB_PASSWORD=quickbite_test
DB_NAME=quickbite_test
```

## 🧪 Running Tests Locally

### Run All Tests

```bash
# From project root
./tests/run_all_tests.sh
```

### Run Individual Test Suites

#### Postman/Newman Tests

```bash
cd tests/postman
chmod +x run_collection.sh
./run_collection.sh

# Or use Newman directly
newman run quickbite-e2e.postman_collection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export ../../reports/newman/report.html
```

**Output**: HTML report in `reports/newman/report.html`

#### Cypress E2E Tests

```bash
cd frontend

# Interactive mode (Cypress Test Runner)
npx cypress open

# Headless mode (CI)
npx cypress run --browser chrome

# Run specific test file
npx cypress run --spec "cypress/e2e/quickbite_e2e.cy.ts"

# With environment variables
CYPRESS_BASE_URL=http://localhost:5173 \
API_BASE_URL=http://localhost:8080 \
npx cypress run
```

**Output**: 
- Videos: `frontend/cypress/videos/`
- Screenshots (on failure): `frontend/cypress/screenshots/`

#### WebSocket Tests

```bash
cd tests/ws

# Run with Jest
npx jest order_updates.test.js --verbose

# With environment variables
API_BASE_URL=http://localhost:8080 \
WS_URL=ws://localhost:8080/ws \
npx jest order_updates.test.js --verbose

# Watch mode for development
npx jest order_updates.test.js --watch
```

#### Webhook Simulator (Manual Testing)

```bash
cd tests/utils

# Make script executable
chmod +x send_webhook.sh

# Edit webhook payload
nano webhook_payment_success.json

# Replace ORDER_ID_PLACEHOLDER with actual order ID
# Then send webhook
./send_webhook.sh webhook_payment_success.json

# Send to custom endpoint
./send_webhook.sh webhook_payment_success.json http://localhost:8080/api/payments/webhook
```

## 🔧 Test Components

### Postman Collection Structure

```
quickbite-e2e.postman_collection.json
├── Health Check
├── Auth
│   ├── Register Customer
│   ├── Register Vendor
│   ├── Register Driver
│   ├── Login Customer
│   ├── Refresh Token
│   └── Protected Endpoint Without Token (401)
├── Vendors
│   ├── List All Vendors
│   ├── Get Vendor By ID
│   └── Search Vendors
├── Menu Items
│   ├── Create Menu Item (Vendor)
│   ├── Get Menu Items By Vendor
│   └── Update Menu Item
├── Orders
│   ├── Create Order (Customer)
│   ├── Get Order By ID
│   ├── Accept Order (Vendor)
│   ├── Update Order Status (Vendor)
│   ├── Assign Driver to Order
│   └── Get Order Status History
└── Payments
    ├── Create Payment Intent
    └── Get Payment By ID
```

### Cypress Test Scenarios

1. **Journey 1: Customer Order Flow**
   - Register new customer
   - Login and browse vendors
   - View menu, add items to cart
   - Create order
   - Create payment intent
   - Simulate webhook (payment success)
   - Verify order and payment status

2. **Journey 2: Vendor Order Management**
   - Login as vendor
   - List vendor orders
   - Accept order
   - Update status: PREPARING → READY
   - CRUD menu items
   - Test invalid status transitions

3. **Journey 3: Driver Delivery**
   - Login as driver
   - Get assigned to order
   - Update status: PICKED_UP → ENROUTE → DELIVERED
   - Verify complete status history

4. **Security & Edge Cases**
   - Unauthorized access attempts
   - Role-based restrictions
   - Invalid data validation
   - Non-existent resource handling

### WebSocket Test Cases

- Connect to WebSocket server
- Subscribe to order topics
- Receive status updates on order changes
- Handle multiple status updates (lifecycle)
- Subscription to non-existent orders
- Connection error handling

### Custom Cypress Commands

```typescript
cy.loginAs('customer')        // Login as test customer
cy.loginAs('vendor')          // Login as test vendor
cy.loginAs('driver')          // Login as test driver
cy.loginViaApi(username, pwd) // Login and store tokens
cy.createOrderViaApi(data)    // Create order via API
cy.simulateWebhook(payload)   // Send signed webhook
cy.waitForApi('@alias')       // Wait for API response
```

## 🔄 CI/CD Integration

### GitHub Actions

The E2E test suite runs automatically on:
- Push to `main` or `develop` branches
- Pull requests targeting `main` or `develop`
- Manual workflow dispatch

**Workflow File**: `.github/workflows/e2e-tests.yml`

#### Workflow Steps

1. **Setup**: Checkout code, install dependencies
2. **Services**: Start PostgreSQL & Redis containers
3. **Seed**: Load test data into database
4. **Backend**: Build and start Spring Boot app
5. **Frontend**: Build and start Vite dev server
6. **Wait**: Health check loops for readiness
7. **Test**: Run Newman, WebSocket, and Cypress tests
8. **Artifacts**: Upload reports, logs, screenshots, videos
9. **Results**: Summarize and fail if any test failed

#### Secrets Required

Configure these in GitHub repository secrets:

- `JWT_SECRET`: JWT signing key (default provided for testing)
- `PAYMENT_WEBHOOK_SECRET`: Webhook HMAC secret

### Running in CI Locally (Docker)

```bash
# Use docker-compose test profile
docker compose -f docker-compose.test.yml up --abort-on-container-exit

# Or build and test
docker compose -f docker-compose.test.yml build
docker compose -f docker-compose.test.yml run --rm e2e-tests
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Backend Not Starting

**Symptom**: Tests fail with connection refused
**Solution**:
```bash
# Check backend logs
tail -f backend.log

# Verify health endpoint
curl http://localhost:8080/actuator/health

# Check port availability
lsof -i :8080
```

#### 2. Frontend Not Loading

**Symptom**: Cypress cannot connect to http://localhost:5173
**Solution**:
```bash
# Check frontend logs
tail -f frontend.log

# Verify frontend is running
curl http://localhost:5173

# Check Vite process
ps aux | grep vite
```

#### 3. WebSocket Connection Failures

**Symptom**: `Could not find a valid Docker environment` or connection timeout
**Solution**:
```bash
# Check WebSocket endpoint
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  http://localhost:8080/ws

# Verify STOMP broker is active (check backend logs)
grep -i "stomp" backend.log
```

#### 4. Database Seed Failures

**Symptom**: Test users not found (401/403 errors)
**Solution**:
```bash
# Re-run seed script
psql -h localhost -U quickbite -d quickbite -f tests/seed/test_seed.sql

# Verify test users exist
psql -h localhost -U quickbite -d quickbite -c \
  "SELECT username, email FROM users WHERE username LIKE 'e2e_test_%';"
```

#### 5. Webhook Signature Mismatch

**Symptom**: Webhook returns 401 "Invalid signature"
**Solution**:
- Verify `PAYMENT_WEBHOOK_SECRET` matches backend config
- Check timestamp is recent (within 5 minutes)
- Ensure payload is identical to what was signed
- Use `send_webhook.sh` script which handles signing correctly

#### 6. Token Expiration

**Symptom**: Tests fail with 401 after some time
**Solution**:
- JWT tokens expire after 1 hour by default
- Use refresh token endpoint to get new access token
- Tests should re-login if needed

#### 7. Cypress Video Recording Issues

**Symptom**: Videos not generated or corrupted
**Solution**:
```bash
# Disable video recording for faster tests
npx cypress run --config video=false

# Or clean cypress cache
npx cypress cache clear
npx cypress install
```

### Enable Debug Logging

#### Backend (application.properties)
```properties
logging.level.com.quickbite=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

#### Cypress
```bash
DEBUG=cypress:* npx cypress run
```

#### WebSocket Tests
- Check `stompClient.debug` output in test logs

### Test Data Cleanup

If tests leave stale data:

```bash
# Clean up test orders
psql -h localhost -U quickbite -d quickbite << EOF
DELETE FROM delivery_status WHERE order_id IN 
  (SELECT id FROM orders WHERE customer_id IN 
    (SELECT id FROM users WHERE username LIKE 'cypress_%'));
DELETE FROM order_items WHERE order_id IN 
  (SELECT id FROM orders WHERE customer_id IN 
    (SELECT id FROM users WHERE username LIKE 'cypress_%'));
DELETE FROM payments WHERE order_id IN 
  (SELECT id FROM orders WHERE customer_id IN 
    (SELECT id FROM users WHERE username LIKE 'cypress_%'));
DELETE FROM orders WHERE customer_id IN 
  (SELECT id FROM users WHERE username LIKE 'cypress_%');
EOF
```

## 📝 Best Practices

### Writing Tests

1. **Use Descriptive Names**: Test names should explain what is being tested
2. **Independent Tests**: Each test should be runnable in isolation
3. **Idempotency**: Tests should produce same result when run multiple times
4. **Cleanup**: Always cleanup test data after completion
5. **Wait for Async**: Use proper waits for async operations (WebSocket messages, API calls)
6. **Assertions**: Make specific assertions, not just status codes
7. **Test Data**: Use seed data for known entities, generate unique data for new entities

### Maintaining Tests

1. **Keep Tests Fast**: Optimize slow tests, parallelize where possible
2. **Retry Logic**: Add retries for flaky network operations
3. **Error Messages**: Provide clear error messages for debugging
4. **Documentation**: Comment complex test logic
5. **Version Control**: Keep test code in sync with API changes

### Running Tests in Development

```bash
# Run only critical tests during development
npx cypress run --spec "cypress/e2e/quickbite_e2e.cy.ts" --grep "Journey 1"

# Run Newman with specific folder
newman run collection.json --folder "Auth"

# Run WebSocket tests in watch mode
cd tests/ws && npx jest --watch
```

## 📊 Test Reports

### Newman HTML Report

Open `reports/newman/report.html` in browser after running Postman tests. Includes:
- Summary dashboard
- Request/response details
- Test assertions (pass/fail)
- Response times

### Cypress Dashboard

Cypress Cloud integration (optional):
```bash
npx cypress run --record --key <your-key>
```

### Custom Test Reporting

Generate JUnit XML for CI integration:

```bash
# Cypress
npx cypress run --reporter junit --reporter-options "mochaFile=reports/cypress-[hash].xml"

# Jest (WebSocket tests)
npx jest --ci --reporters=default --reporters=jest-junit
```

## 🔒 Security Considerations

- **Secrets**: Never commit secrets to version control
- **Test Credentials**: Use separate credentials for testing (not production)
- **Webhook Signatures**: Always verify webhook signatures in tests
- **JWT Tokens**: Rotate test tokens regularly
- **Database**: Use separate test database, never test against production

## 📚 Additional Resources

- [Postman Collection Format](https://schema.getpostman.com/)
- [Newman CLI Documentation](https://learning.postman.com/docs/running-collections/using-newman-cli/)
- [Cypress Best Practices](https://docs.cypress.io/guides/references/best-practices)
- [@stomp/stompjs Documentation](https://github.com/stomp-js/stompjs)
- [GitHub Actions Workflow Syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)

## 🤝 Contributing

When adding new tests:

1. Follow existing test structure and naming conventions
2. Update this README with new test coverage
3. Ensure tests pass locally before committing
4. Add test data to seed script if needed
5. Document any new custom commands or utilities

## 📞 Support

For issues or questions:
- Check [Troubleshooting](#troubleshooting) section
- Review backend logs: `backend.log`
- Review test output and reports
- Open an issue with test logs and error messages

---

**Last Updated**: February 19, 2026  
**Test Suite Version**: 1.0.0
