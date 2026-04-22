# ADR-006: Hexagonal Architecture for Complex Services

## Status
Accepted

## Context
We needed to decide on internal architecture patterns for our 6 microservices. Services have varying levels of business logic complexity:
- **High**: Order Service (state machine, saga, outbox), Payment Service (provider integration, compensating transactions)
- **Low**: User Service (CRUD + auth), Restaurant Service (CRUD + JSONB), Dispatch Service (Go), Notification Service (consume → push)

## Decision
Apply architecture patterns proportional to service complexity:
- **Order & Payment Services**: Hexagonal Architecture (Ports & Adapters)
- **User & Restaurant Services**: Simplified Layered Architecture
- **Dispatch Service**: Idiomatic Go with clean separation
- **Notification Service**: Simple modular TypeScript

## Consequences

### Positive
- **Testability**: Domain layer in Hexagonal services has zero framework dependencies → unit tests run < 1 second
- **Swappable adapters**: Payment gateway can be swapped (Mock → Stripe) by changing config, not business logic
- **Not over-engineering**: Simple services stay simple (Layered pattern)
- **Right tool for the job**: Each pattern matches the service's actual complexity

### Negative
- Slight inconsistency between Java services (2 patterns)
- Hexagonal has a higher learning curve

### Mitigations
- Order Service skeleton provides complete example of Hexagonal structure
- Domain package rule is simple: "No Spring/JPA/Kafka imports allowed"
- Start with User Service (Layered, simpler) before tackling Order Service (Hexagonal)
