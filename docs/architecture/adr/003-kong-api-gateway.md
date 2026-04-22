# ADR-003: Kong as API Gateway

## Status
Accepted

## Context
The system needs an API Gateway for:
- Single entry point for external clients (mobile, web)
- JWT authentication and token validation
- Rate limiting
- Request routing to internal services
- Observability (access logs, metrics)

Options considered: Kong, Nginx Ingress + custom auth, Traefik, Spring Cloud Gateway.

## Decision
We chose **Kong Ingress Controller** deployed on Kubernetes.

## Consequences

### Positive
- **Plugin ecosystem**: JWT validation, rate limiting, Prometheus metrics, OpenTelemetry — all out of the box
- **K8s native**: KongIngress CRDs for declarative configuration
- **Performance**: Built on Nginx, handles high throughput
- **Single responsibility**: Auth validation happens at gateway, services trust headers (`X-User-Id`, `X-User-Role`)

### Negative
- Additional infrastructure component to manage
- Learning curve for Kong plugin configuration

### Auth Flow
```
Client → Kong (JWT validation) → Inject X-User-Id, X-User-Role headers → Service
```

- Services do NOT re-validate JWT tokens
- Internal service-to-service calls bypass Kong (via K8s DNS directly)
- Internal endpoints (`/api/internal/*`) are NOT registered in Kong routes
