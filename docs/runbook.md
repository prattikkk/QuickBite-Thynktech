# QuickBite — Operations Runbook

> Procedures for common operational tasks. Keep this document updated as infrastructure evolves.

## Table of Contents
1. [Service Restart](#service-restart)
2. [Database Backup & Restore](#database-backup--restore)
3. [Log Access](#log-access)
4. [Secret Rotation](#secret-rotation)
5. [Scaling](#scaling)
6. [Incident Response](#incident-response)
7. [Monitoring Alerts](#monitoring-alerts)
8. [Common Issues & Fixes](#common-issues--fixes)

---

## Service Restart

### Docker Compose

```bash
# Restart a single service
docker compose -f docker-compose.prod.yml restart backend

# Full stack restart
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### Kubernetes

```bash
# Rolling restart (zero-downtime)
kubectl -n quickbite rollout restart deployment/quickbite-backend

# Force kill and recreate a specific pod
kubectl -n quickbite delete pod <pod-name>
```

---

## Database Backup & Restore

### Scheduled Backup

```bash
# Manual backup
./ops/backup-db.sh

# Cron (daily at 2 AM)
0 2 * * * cd /opt/quickbite && ./ops/backup-db.sh >> /var/log/quickbite-backup.log 2>&1
```

### Restore

```bash
# ⚠️ Destructive — drops and recreates DB
./ops/restore-db.sh backups/quickbite_20250101_120000.sql.gz
```

### Verify Backup Integrity

```bash
# Test restore on a throwaway DB
DB_NAME=quickbite_test ./ops/restore-db.sh backups/quickbite_20250101_120000.sql.gz

# Check schema
psql -h localhost -U quickbite -d quickbite_test \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"

# Cleanup
psql -h localhost -U quickbite -d postgres -c "DROP DATABASE quickbite_test;"
```

---

## Log Access

### Docker Compose

```bash
# Tail all logs
docker compose -f docker-compose.prod.yml logs -f --tail=100

# Specific service
docker compose -f docker-compose.prod.yml logs -f backend --since=1h

# Search for errors
docker compose -f docker-compose.prod.yml logs backend | grep -i "error\|exception"
```

### Kubernetes

```bash
# Current pod logs
kubectl -n quickbite logs -f deployment/quickbite-backend --tail=100

# Previous crashed pod
kubectl -n quickbite logs <pod-name> --previous

# All pods
kubectl -n quickbite logs -l app=quickbite-backend --tail=50
```

---

## Secret Rotation

### JWT Secret

1. Generate a new secret:
   ```bash
   NEW_SECRET=$(openssl rand -hex 32)
   ```

2. **Docker Compose:**
   ```bash
   # Update .env.prod
   sed -i "s/^JWT_SECRET=.*/JWT_SECRET=${NEW_SECRET}/" .env.prod

   # Restart backend
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps backend
   ```

3. **Kubernetes:**
   ```bash
   kubectl -n quickbite create secret generic quickbite-secrets \
     --from-literal=JWT_SECRET="${NEW_SECRET}" \
     --dry-run=client -o yaml | kubectl apply -f -

   kubectl -n quickbite rollout restart deployment/quickbite-backend
   ```

4. **Impact:** All existing JWT tokens become invalid. Users must re-login.

### Database Password

1. Update password in PostgreSQL
2. Update `DB_PASSWORD` in `.env.prod` or K8s secret
3. Restart backend
4. Verify: `./ops/healthcheck.sh`

### Payment Webhook Secret

1. Rotate in Razorpay/Stripe dashboard
2. Update `PAYMENT_WEBHOOK_SECRET` in env/secrets
3. Restart backend
4. Test with `./ops/healthcheck.sh`

---

## Scaling

### Horizontal (more pods/containers)

```bash
# Kubernetes
kubectl -n quickbite scale deployment/quickbite-backend --replicas=5

# Docker Compose (limited)
docker compose -f docker-compose.prod.yml up -d --scale backend=3
```

### Vertical (more resources)

Update resource requests/limits in `k8s/deploy.yaml`:
```yaml
resources:
  requests:
    cpu: 500m
    memory: 1Gi
  limits:
    cpu: "2"
    memory: 2Gi
```

---

## Incident Response

### Severity Levels

| Level | Description | Response Time | Example |
|-------|-------------|---------------|---------|
| P1 | Service down | 15 min | Backend crash loop, DB unreachable |
| P2 | Degraded | 1 hour | Payment failures, slow responses |
| P3 | Minor | 4 hours | UI glitch, non-critical feature broken |

### P1 Checklist

1. **Verify:** `./ops/healthcheck.sh`
2. **Check logs:** `docker compose logs backend --tail=200`
3. **Check DB:** `psql -h <host> -U quickbite -c "SELECT 1;"`
4. **Check Redis:** `redis-cli -h <host> ping`
5. **Rollback if needed:** See [Deployment Guide — Rollback](deployment.md#rollback-procedure)
6. **Notify stakeholders**
7. **Post-incident:** Write post-mortem within 24h

---

## Monitoring Alerts

| Alert | Condition | Action |
|-------|-----------|--------|
| Backend Down | `/actuator/health` returns non-200 for 2 min | Check logs, restart pod |
| High Latency | p99 > 2s for 5 min | Check DB connections, scale up |
| Payment Failure Rate | > 5% failures in 10 min | Check payment provider status, logs |
| Webhook Errors | > 10 errors/min | Verify webhook secret, check signature validation |
| Disk Usage > 85% | DB volume filling up | Run backup + prune old data |
| OOM Killed | Container exits with OOM | Increase memory limits |
| WebSocket Disconnects | > 50% drop in active connections | Check Redis pub/sub, backend health |

---

## Common Issues & Fixes

### Backend won't start — "Connection refused" to DB

```bash
# Check DB is running
docker compose -f docker-compose.prod.yml ps postgres
# or
kubectl -n quickbite get pods -l app=postgres

# Check connection string
docker compose -f docker-compose.prod.yml exec backend printenv | grep DATASOURCE
```

### Flyway migration conflict

```bash
# Check migration status
psql -U quickbite -d quickbite \
  -c "SELECT version, description, success FROM flyway_schema_history;"

# If a failed migration needs cleanup:
psql -U quickbite -d quickbite \
  -c "DELETE FROM flyway_schema_history WHERE success = false;"

# Then restart backend to retry
```

### WebSocket connections timing out

- Verify nginx proxy timeouts are set (60s+ for WebSocket)
- Check `proxy_read_timeout` and `proxy_send_timeout` in `frontend/nginx.conf`
- For K8s, check ingress annotations: `nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"`

### Payment webhook returning 400

1. Check HMAC signature is computed correctly
2. Verify `PAYMENT_WEBHOOK_SECRET` matches the provider dashboard
3. Check request body encoding (raw body, not parsed JSON)
4. Test: `curl -X POST http://localhost:8080/api/payments/webhook -H "Content-Type: application/json" -d '{}'`

### Frontend shows blank page

```bash
# Verify nginx is serving files
docker exec quickbite-frontend ls /usr/share/nginx/html/

# Check nginx config
docker exec quickbite-frontend nginx -t

# Check SPA fallback
curl -I http://localhost/some/deep/route  # Should return 200
```

---

*Last updated: Day 6 — Production Readiness*
