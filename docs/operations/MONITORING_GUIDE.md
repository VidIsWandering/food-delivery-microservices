# Monitoring Guide

## 1. Observability Stack

| Component | Tool | Access |
|-----------|------|--------|
| Metrics | Prometheus + Grafana | `kubectl port-forward svc/grafana 3000:3000 -n observability` |
| Logs | Promtail + Loki + Grafana | Same Grafana instance, Loki data source |
| Traces | OpenTelemetry + Jaeger | `kubectl port-forward svc/jaeger-query 16686:16686 -n observability` |
| Alerts | Alertmanager | Integrated with Prometheus |

## 2. Key Dashboards (Grafana)

### System Overview Dashboard
- Total request rate (req/sec) across all services
- Error rate (%) — target: < 1%
- P50 / P95 / P99 latency — target: P99 < 500ms
- Active pods per service

### Per-Service Dashboard
- HTTP request rate by endpoint
- Response time histogram
- Error breakdown by status code
- CPU & Memory usage
- JVM metrics (Java): heap usage, GC pauses
- Go metrics: goroutine count, GC stats

### Kafka Dashboard
- Messages produced/consumed per topic
- Consumer group lag (critical metric!)
- Broker disk usage
- Partition distribution

### Business Dashboard
- Orders created per minute
- Order completion rate (delivered / total)
- Average delivery time (minutes)
- Payment success/failure ratio
- Top restaurants by order count

## 3. SLI/SLO Definitions

| Service | SLI (Indicator) | SLO (Target) |
|---------|-----------------|---------------|
| Kong Gateway | Request success rate | 99.5% |
| Order Service | P99 latency (POST /orders) | < 500ms |
| Order Service | Order creation success rate | > 99% |
| Payment Service | Payment processing success rate | > 98% |
| Dispatch Service | Driver matching success rate | > 95% |
| Dispatch Service | GPS update latency | < 100ms |
| All Services | Pod restart count per hour | < 2 |
| Kafka | Consumer lag (messages) | < 1000 |

## 4. Alert Rules

### Critical (PagerDuty / Immediate action)

| Alert | Condition | Action |
|-------|-----------|--------|
| Service Down | Pod restart > 3 in 5min | Check logs, rollback |
| Error Rate Spike | HTTP 5xx > 5% for 2min | Check Jaeger traces |
| Kafka Consumer Lag | Lag > 10000 for 5min | Scale consumers |
| DB Connection Pool Exhausted | Active connections = max | Check for connection leaks |

### Warning (Slack notification)

| Alert | Condition | Action |
|-------|-----------|--------|
| High Latency | P99 > 1s for 5min | Investigate slow endpoints |
| High Memory | Memory > 85% limit | Consider scaling |
| Disk Usage | PV usage > 80% | Clean up or expand |
| Certificate Expiry | TLS cert expires in < 14 days | Renew certificate |

## 5. Structured Logging Standard

All services MUST output logs in this JSON format:

```json
{
  "timestamp": "2026-04-22T08:00:00Z",
  "level": "INFO",
  "service": "order-service",
  "trace_id": "abc123def456",
  "span_id": "span789",
  "message": "Order created successfully",
  "order_id": "ord-uuid-123",
  "customer_id": "usr-uuid-456",
  "duration_ms": 45
}
```

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | ISO 8601 | When the log was created |
| `level` | string | ERROR, WARN, INFO, DEBUG |
| `service` | string | Service name |
| `trace_id` | string | OpenTelemetry trace ID (for cross-service correlation) |
| `message` | string | Human-readable description |

### Searching Logs in Grafana (Loki)

```
# All logs from order-service
{service="order-service"}

# Error logs only
{service="order-service"} |= "ERROR"

# Find all logs for a specific trace
{service=~".+"} |= "abc123def456"

# Find logs for a specific order
{service="order-service"} | json | order_id = "ord-uuid-123"
```

## 6. Distributed Tracing (Jaeger)

### How Traces Work

1. Customer request hits **Kong Gateway** → generates `trace_id`
2. Kong passes `trace_id` via `traceparent` header to downstream service
3. Each service propagates `trace_id` to:
   - Internal REST calls (via HTTP header)
   - Kafka messages (via message header)
   - Log entries (via MDC/context)
4. Jaeger UI shows complete request flow across all services

### Jaeger Usage

```
1. Open Jaeger UI (localhost:16686)
2. Select service (e.g., "order-service")
3. Search by trace ID, operation, or time range
4. Click on a trace to see span waterfall
5. Identify bottlenecks (which span took longest?)
```
