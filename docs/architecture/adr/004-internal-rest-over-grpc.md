# ADR-004: Internal REST over gRPC

## Status
Accepted

## Context
We needed to choose a protocol for synchronous inter-service communication (service-to-service queries). The two main options were gRPC (Protocol Buffers) and Internal REST (JSON over HTTP).

## Decision
We chose **Internal REST (JSON)** for all synchronous inter-service communication.

## Consequences

### Positive
- **Zero additional tooling**: No need for protoc, buf, or code generation plugins for 3 languages (Java, Go, Node.js)
- **Easy debugging**: Can test with curl, Postman, or browser
- **Lower learning curve**: Team is already proficient with REST
- **Simpler CI**: No proto generation step required

### Negative
- Slightly higher latency (~20-50ms vs ~5-20ms for gRPC binary) — acceptable for our scale
- No automatic type safety from proto files — mitigated by OpenAPI specs

### Mitigations
- OpenAPI YAML specs serve as API contracts (source of truth)
- Kubernetes NetworkPolicy restricts internal endpoints to cluster-only access
- Internal endpoints use `/api/internal/` prefix, not routed through Kong
