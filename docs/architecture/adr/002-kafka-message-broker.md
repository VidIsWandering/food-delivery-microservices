# ADR-002: Apache Kafka as Message Broker

## Status
Accepted

## Context
The system requires asynchronous, event-driven communication between services. Key requirements:
- Saga orchestration (Order → Payment → Restaurant → Dispatch)
- Transactional Outbox Pattern for reliable event publishing
- Event replay capability for debugging and recovery
- At-least-once delivery guarantee

Options considered: RabbitMQ, Apache Kafka, Redis Streams.

## Decision
We chose **Apache Kafka** (managed via Strimzi operator on Kubernetes).

## Consequences

### Positive
- **Durable event log**: Messages persisted for 7 days, enabling replay
- **Consumer groups**: Each service consumes independently at its own pace
- **Partitioning**: Horizontal scalability (6 partitions per topic)
- **CloudEvents format**: Standardized event envelope with `id`, `type`, `source`, `time`
- **Idempotency**: Event `id` enables consumer-side deduplication
- **Strimzi**: K8s-native operator, declarative topic management via CRDs

### Negative
- Higher operational complexity vs RabbitMQ
- Requires Zookeeper (being removed in KRaft mode in future)
- Higher memory footprint

### Topic Design

| Topic | Producers | Consumers |
|-------|-----------|-----------|
| `order-events` | Order Service | Payment, Notification |
| `payment-events` | Payment Service | Order, Restaurant, Notification |
| `restaurant-events` | Restaurant Service | Order, Dispatch, Notification |
| `delivery-events` | Dispatch Service | Order, Notification |
