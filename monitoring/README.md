# QuickBite — Monitoring & Alerting

> Prometheus + Grafana stack for production observability.

---

## Architecture

```
Backend (Spring Boot Actuator)
  └─ /actuator/prometheus  →  Prometheus (scrape every 15s)
                                   └─ Grafana (dashboards + alerts)
```

---

## 1. Spring Boot Actuator Setup

Already configured in `application.properties`:

```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.metrics.export.prometheus.enabled=true
management.endpoint.health.show-details=when-authorized
```

Add the Prometheus dependency if not present in `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## 2. Prometheus Configuration

### prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

scrape_configs:
  - job_name: "quickbite-backend"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["backend:8080"]
    # For K8s, use kubernetes_sd_configs instead

  - job_name: "postgres"
    static_configs:
      - targets: ["postgres-exporter:9187"]

  - job_name: "redis"
    static_configs:
      - targets: ["redis-exporter:9121"]

  - job_name: "nginx"
    static_configs:
      - targets: ["nginx-exporter:9113"]
```

### Add to docker-compose.prod.yml (optional)

```yaml
  prometheus:
    image: prom/prometheus:v2.48.0
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - quickbite

  grafana:
    image: grafana/grafana:10.2.0
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-admin}
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    networks:
      - quickbite
```

---

## 3. Alert Rules

### alert_rules.yml

```yaml
groups:
  - name: quickbite-alerts
    rules:
      # ── Service Health ─────────────────────────────
      - alert: BackendDown
        expr: up{job="quickbite-backend"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "QuickBite backend is down"
          description: "Backend has been unreachable for 2 minutes."

      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          /
          sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Error rate above 5%"

      # ── Latency ────────────────────────────────────
      - alert: HighLatency
        expr: |
          histogram_quantile(0.99,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "p99 latency above 2 seconds"

      # ── Payment ────────────────────────────────────
      - alert: PaymentFailureRate
        expr: |
          sum(rate(http_server_requests_seconds_count{uri="/api/payments/webhook",status=~"4..|5.."}[10m]))
          /
          sum(rate(http_server_requests_seconds_count{uri="/api/payments/webhook"}[10m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Payment webhook failure rate above 5%"

      # ── JVM ────────────────────────────────────────
      - alert: HighHeapUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          /
          jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap usage above 90%"

      # ── Database ───────────────────────────────────
      - alert: DBConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "DB connection pool > 90% utilized"

      # ── WebSocket ──────────────────────────────────
      - alert: WebSocketConnectionDrop
        expr: |
          delta(spring_websocket_sessions_active[5m]) < -50
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Large drop in WebSocket connections"
```

---

## 4. Grafana Dashboards

### Recommended Dashboards

| Dashboard | Grafana ID | Description |
|-----------|------------|-------------|
| Spring Boot | 12900 | JVM, HTTP, Hikari pool |
| PostgreSQL | 9628 | Connections, queries, locks |
| Redis | 11835 | Memory, keys, commands |
| Nginx | 12708 | Requests, latency, errors |

### Import Steps

1. Go to Grafana → Dashboards → Import
2. Enter dashboard ID
3. Select Prometheus data source
4. Save

### Custom QuickBite Dashboard Panels

Create a dashboard with these panels:

| Panel | Query | Type |
|-------|-------|------|
| Request Rate | `sum(rate(http_server_requests_seconds_count[5m])) by (uri)` | Time series |
| Error Rate | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))` | Stat (red threshold) |
| p95 Latency | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | Time series |
| Active Orders | `quickbite_orders_active` (custom metric) | Gauge |
| Payment Success | `sum(rate(http_server_requests_seconds_count{uri=~"/api/payments.*",status="200"}[5m]))` | Stat (green) |
| DB Connections | `hikaricp_connections_active` | Gauge |
| JVM Heap | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100` | Gauge (%) |
| WebSocket Sessions | `spring_websocket_sessions_active` | Time series |

---

## 5. Key Metrics Reference

### Application Metrics (from Actuator/Micrometer)

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds_count` | Total HTTP requests |
| `http_server_requests_seconds_sum` | Total request duration |
| `http_server_requests_seconds_bucket` | Request duration histogram |
| `jvm_memory_used_bytes` | JVM memory usage |
| `jvm_gc_pause_seconds_count` | GC pause count |
| `hikaricp_connections_active` | Active DB connections |
| `hikaricp_connections_idle` | Idle DB connections |
| `spring_websocket_sessions_active` | Active WebSocket sessions |

### Business Metrics (custom — add via Micrometer)

```java
// Example: Track order creation
@Autowired MeterRegistry meterRegistry;

meterRegistry.counter("quickbite.orders.created", "vendor", vendorName).increment();
meterRegistry.timer("quickbite.payments.duration").record(duration);
```

---

*Last updated: Day 6 — Production Readiness*
