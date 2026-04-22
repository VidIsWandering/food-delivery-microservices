# ADR-005: PostgreSQL JSONB over MongoDB

## Status
Accepted

## Context
Restaurant Service needs to store flexible menu structures (categories → items → options). Options vary per item and have nested structures. The choices were MongoDB (document DB) or PostgreSQL with JSONB columns.

## Decision
We chose **PostgreSQL with JSONB** for all services, including Restaurant Service's menu data.

## Consequences

### Positive
- **Reduced operational complexity**: 1 database engine instead of 2 (PostgreSQL + MongoDB → just PostgreSQL)
- **Unified backup/monitoring**: Single pipeline for all databases
- **DevOps burden reduced**: One less stateful service to manage on K8s
- **Development simplicity**: Spring Data JPA only, no need for Spring Data MongoDB
- **JSONB is powerful enough**: Supports indexing, querying, and operators comparable to MongoDB for our use case

### Negative
- Less natural for deeply nested document queries
- No MongoDB-specific features (change streams, aggregation pipeline)

### Mitigations
- Use Hibernate Types library for seamless JSONB ↔ Java object mapping
- Create GIN indexes on JSONB columns for query performance
- Menu data is bounded in size (not a scalability concern)
