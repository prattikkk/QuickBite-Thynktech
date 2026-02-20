# QuickBite — Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Docker Compose (Staging / Single-Host)](#docker-compose-staging--single-host)
4. [Kubernetes (Production)](#kubernetes-production)
5. [Flyway Migrations](#flyway-migrations)
6. [SSL / TLS](#ssl--tls)
7. [Rollback Procedure](#rollback-procedure)

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 24+ | `docker compose` v2 plugin required |
| kubectl | 1.28+ | For K8s deployments |
| PostgreSQL client | 13+ | `psql`, `pg_dump` for ops scripts |
| Node.js | 20 LTS | Only if building frontend locally |
| Java | 17 | Only if building backend locally |
| Maven | 3.9+ | Only if building backend locally |

---

## Environment Setup

1. **Copy and fill secrets**
   ```bash
   cp .env.prod.example .env.prod
   # Edit .env.prod with real credentials
   ```

2. **Required secrets:**
   - `DB_USERNAME` / `DB_PASSWORD` — PostgreSQL credentials
   - `JWT_SECRET` — min 64-char random string (e.g. `openssl rand -hex 32`)
   - `PAYMENT_WEBHOOK_SECRET` — from Razorpay/Stripe dashboard
   - `CORS_ORIGINS` — e.g. `https://quickbite.example.com`

---

## Docker Compose (Staging / Single-Host)

### First Deploy

```bash
# Pull/build images
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# Verify services
docker compose -f docker-compose.prod.yml ps

# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend
curl -I http://localhost:80
```

### Update (Rolling)

```bash
# Pull latest images
docker compose -f docker-compose.prod.yml --env-file .env.prod pull

# Recreate only changed services (zero-downtime for stateless)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps frontend
```

### View Logs

```bash
# All services
docker compose -f docker-compose.prod.yml logs -f

# Specific service
docker compose -f docker-compose.prod.yml logs -f backend
```

### Stop

```bash
docker compose -f docker-compose.prod.yml down
# Add -v to also remove named volumes (⚠️ data loss!)
```

---

## Kubernetes (Production)

### Initial Setup

```bash
# Create namespace
kubectl apply -f k8s/deploy.yaml

# Create secrets
kubectl create secret generic quickbite-secrets -n quickbite \
  --from-literal=DB_USERNAME=dbuser \
  --from-literal=DB_PASSWORD='<password>' \
  --from-literal=JWT_SECRET='<secret>' \
  --from-literal=PAYMENT_WEBHOOK_SECRET='<secret>'

# Deploy everything
kubectl apply -f k8s/deploy.yaml

# Watch rollout
kubectl -n quickbite rollout status deployment/quickbite-backend
kubectl -n quickbite rollout status deployment/quickbite-frontend
```

### Update Image

```bash
# Update to specific SHA
kubectl -n quickbite set image deployment/quickbite-backend \
  backend=ghcr.io/<your-org>/quickbite/backend:<sha>

# Monitor rollout
kubectl -n quickbite rollout status deployment/quickbite-backend
```

### Scaling

```bash
# Scale backend
kubectl -n quickbite scale deployment/quickbite-backend --replicas=5

# Enable HPA (requires metrics-server)
kubectl -n quickbite autoscale deployment/quickbite-backend \
  --min=3 --max=10 --cpu-percent=70
```

### Debugging

```bash
# Pod status
kubectl -n quickbite get pods -o wide

# Logs
kubectl -n quickbite logs -f deployment/quickbite-backend

# Shell into pod
kubectl -n quickbite exec -it deployment/quickbite-backend -- sh
```

---

## Flyway Migrations

Flyway runs automatically on backend startup (`spring.flyway.enabled=true`).

### Current Migrations

| Version | Description |
|---------|-------------|
| V1 | Initial schema — users, roles |
| V2 | Vendors, menu items, categories |
| V3 | Orders, order items, addresses |
| V4 | Payments (Razorpay/Stripe), idempotency keys |
| V5 | Indexes, constraints, audit columns |

### Manual Migration

```bash
# Connect to the DB and check status
docker exec -it quickbite-postgres psql -U quickbite -d quickbite \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

### Rollback Strategy

Flyway Community does not support `undo`. To roll back:
1. Take a DB backup: `./ops/backup-db.sh`
2. Deploy the previous backend image (contains the older migration set)
3. If schema is incompatible, restore from backup: `./ops/restore-db.sh <file>`

---

## SSL / TLS

### Docker Compose (with Caddy or Traefik)

Add a reverse proxy in front of the frontend. Recommended: **Caddy** (auto-HTTPS):

```
# Caddyfile snippet
quickbite.example.com {
    reverse_proxy frontend:80
}
```

### Kubernetes (cert-manager)

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml

# Uncomment TLS sections in k8s/deploy.yaml ingress
# Create ClusterIssuer for Let's Encrypt
```

---

## Rollback Procedure

### Docker Compose

```bash
# 1. Backup current state
./ops/backup-db.sh

# 2. Roll back to previous image tag
export TAG=<previous-sha>
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps backend

# 3. Verify
curl http://localhost:8080/actuator/health
./ops/healthcheck.sh
```

### Kubernetes

```bash
# 1. Backup DB
./ops/backup-db.sh

# 2. Undo last rollout
kubectl -n quickbite rollout undo deployment/quickbite-backend

# 3. Or roll back to specific revision
kubectl -n quickbite rollout history deployment/quickbite-backend
kubectl -n quickbite rollout undo deployment/quickbite-backend --to-revision=<N>

# 4. Verify
kubectl -n quickbite rollout status deployment/quickbite-backend
./ops/healthcheck.sh http://<ingress-ip>:8080
```

---

## Environment Matrix

| Variable | Dev | Staging | Production |
|----------|-----|---------|------------|
| `SPRING_PROFILES_ACTIVE` | dev | staging | prod |
| `DB_HOST` | localhost | postgres | managed-db-host |
| `CORS_ORIGINS` | `*` | staging URL | production URL |
| `JWT_SECRET` | dev-secret | random | random (rotated) |
| `PAYMENT_PROVIDER` | mock | razorpay (test) | razorpay (live) |

---

*Last updated: Day 6 — Production Readiness*
