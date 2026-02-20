# QuickBite - Non-Functional Requirements

## NFR-1: Performance

### Response Time
- **Target**: 95th percentile response time < 300ms for read operations
- **Target**: 95th percentile response time < 500ms for write operations
- **Measurement**: Application Performance Monitoring (APM) with Prometheus

### Database Query Performance
- Vendor search queries: < 200ms (with PostGIS spatial index)
- Order creation: < 400ms (includes payment intent)
- Menu retrieval: < 100ms (with Redis caching)

### Throughput
- Support 100 concurrent users during MVP
- Target: 500 orders per hour per region
- API rate limit: 100 requests/minute per user

**Acceptance Criteria**:
- Load tests pass with 100 concurrent users (JMeter/k6)
- Database indexes on foreign keys and search columns
- Redis cache hit rate > 70% for vendor/menu queries
- API responses include `X-Response-Time` header

---

## NFR-2: Availability

### Uptime Target
- **SLA**: 99.5% uptime (≈ 3.6 hours downtime/month)
- Maintenance windows: Sundays 2-4 AM UTC

### Fault Tolerance
- Database: PostgreSQL streaming replication (1 standby)
- Redis: Persistence enabled (RDB snapshots every 5 minutes)
- Application: Minimum 2 instances behind load balancer

### Health Checks
- Endpoint: `GET /actuator/health`
- Checks: DB connection, Redis connection, disk space
- Frequency: Every 30 seconds
- Unhealthy threshold: 3 consecutive failures

**Acceptance Criteria**:
- Automated failover for database (< 30 seconds)
- Circuit breaker pattern for external APIs (Stripe, geocoding)
- Graceful degradation: cart works without Redis (fallback to session)

---

## NFR-3: Scalability

### Horizontal Scaling
- Application: Stateless design, scale to 5+ instances
- Database: Read replicas for analytics queries
- Redis: Cluster mode for > 10K concurrent sessions

### Data Volume
- Expected: 1M users, 10K vendors, 50K orders/month in year 1
- Storage: 100GB database, 500GB object storage

### Scaling Triggers
- Auto-scale when CPU > 70% for 5 minutes
- Database connections pooled (HikariCP max 20 per instance)

**Acceptance Criteria**:
- Application instances are stateless (JWT validation, no server-side sessions)
- Database connection pool tuned (max_connections = 100)
- CDN for static assets (vendor logos, menu images)

---

## NFR-4: Security

### Authentication & Authorization
- **JWT**: HS256 algorithm, 256-bit secret key
- **RBAC**: Roles enforced at controller level (`@PreAuthorize`)
- **Password Policy**: Min 8 chars, bcrypt strength 12

### OWASP Top 10 Mitigations
| Vulnerability | Mitigation |
|---------------|------------|
| Injection | Parameterized queries (JPA), input validation |
| Broken Auth | JWT expiration, refresh token rotation |
| XSS | Content Security Policy, React auto-escaping |
| CSRF | SameSite cookies, CSRF tokens for state-changing ops |
| Insecure Deserialization | Avoid deserializing untrusted data |
| Using Components with Known Vulnerabilities | Snyk/Dependabot scans |

### Data Protection
- **PII**: Encrypt email, phone at rest (AES-256)
- **PCI-DSS**: Never store card numbers (use Stripe tokens)
- **HTTPS**: TLS 1.2+ required

**Acceptance Criteria**:
- All endpoints except `/api/auth/login` require valid JWT
- SQL injection tests pass (sqlmap)
- Secrets stored in environment variables, not code
- Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`

---

## NFR-5: Observability

### Logging
- **Format**: JSON structured logs
- **Levels**: INFO (default), DEBUG (dev), ERROR (always)
- **Fields**: timestamp, traceId, userId, endpoint, duration, status

### Metrics
- **Collection**: Micrometer + Prometheus
- **Key Metrics**:
  - Request rate, error rate, duration (RED method)
  - JVM heap usage, GC pauses
  - Database connection pool usage
  - Order conversion funnel

### Tracing
- **Tool**: Spring Cloud Sleuth (optional)
- Trace ID propagated across service boundaries (future microservices)

### Alerting
- P1: API error rate > 5% for 5 minutes
- P2: Database connection pool exhausted
- P3: Disk usage > 80%

**Acceptance Criteria**:
- Logs aggregated to ELK or CloudWatch
- Prometheus scrapes `/actuator/prometheus` endpoint
- Grafana dashboards for key metrics
- Exception stack traces logged with context

---

## NFR-6: Maintainability

### Code Quality
- **Test Coverage**: > 80% line coverage (JUnit, Mockito)
- **Code Style**: Enforce with Checkstyle/SpotBugs
- **SonarQube**: No critical/blocker issues

### Documentation
- API: OpenAPI 3.0 spec auto-generated (Springdoc)
- Code: JavaDoc for public interfaces
- README: Setup instructions, architecture diagram

### Database Migrations
- **Tool**: Flyway
- **Process**: Versioned migrations (V1__init.sql, V2__add_ratings.sql)
- **Rollback**: Write down-migration scripts for critical changes

**Acceptance Criteria**:
- CI pipeline fails if tests fail or coverage drops
- All REST endpoints documented in Swagger UI
- Database migrations run automatically on startup

---

## NFR-7: Deployability

### CI/CD Pipeline
- **Build**: GitHub Actions / GitLab CI
- **Stages**: Compile → Test → Build Docker Image → Deploy

### Environments
- **Dev**: Auto-deploy on merge to `develop`
- **Staging**: Manual approval, smoke tests
- **Prod**: Blue-green deployment, manual approval

### Rollback
- Keep last 3 Docker images
- Rollback time: < 5 minutes
- Database migrations backward-compatible for 1 release

**Acceptance Criteria**:
- Zero-downtime deployments (rolling updates)
- Health checks prevent routing to unhealthy instances
- Feature flags for risky features (LaunchDarkly or environment vars)

---

## NFR-8: Usability (Frontend)

### Response Time
- Page load: < 2 seconds (Lighthouse score > 80)
- Time to Interactive (TTI): < 3 seconds

### Accessibility
- WCAG 2.1 Level AA compliance
- Keyboard navigation support
- ARIA labels for screen readers

### Browser Support
- Chrome, Firefox, Safari, Edge (latest 2 versions)
- Mobile: iOS Safari 13+, Chrome Android

**Acceptance Criteria**:
- Lighthouse audit passes (Performance, Accessibility, Best Practices)
- Mobile-responsive design (breakpoints: 375px, 768px, 1024px)

---

## NFR-9: Compliance

### Data Privacy
- **GDPR**: Right to access, delete, port data
- **CCPA**: Opt-out of data sale (not applicable for MVP)

### Audit Trail
- Log all order state changes with actor + timestamp
- Retain logs for 2 years

**Acceptance Criteria**:
- Endpoint: `DELETE /api/users/me` (GDPR erasure)
- Privacy policy and terms of service linked
- Data retention policy enforced (automated deletion after 7 years)

---

## Summary

| NFR Category | Priority | Owner |
|--------------|----------|-------|
| Performance | High | Backend Team |
| Availability | High | DevOps |
| Scalability | Medium | Backend + DevOps |
| Security | High | Full Stack + Security Lead |
| Observability | Medium | DevOps |
| Maintainability | High | Full Stack |
| Deployability | High | DevOps |
| Usability | Medium | Frontend Team |
| Compliance | Low (MVP) | Product + Legal |

**Review Cycle**: Quarterly reassessment of NFRs as system scales
