# ADR-001: Polyglot Monorepo Strategy

## Status
Accepted

## Context
The team needs to decide between a monorepo (all services in one Git repository) or multi-repo (one repo per service). Team size is 2 developers.

## Decision
We chose a **polyglot monorepo** where all 6 microservices, deployment configs, documentation, and CI/CD pipelines live in a single Git repository.

## Consequences

### Positive
- **Atomic changes**: Cross-service changes (API contract, shared schema) can be done in a single PR
- **Unified CI/CD**: One place to manage all pipelines
- **Discoverability**: New team members can find everything in one place
- **Consistent tooling**: `.editorconfig`, linting, commit conventions apply globally

### Negative
- Repository grows larger over time
- CI must be smart enough to only build changed services (solved with path-based triggers)

### Mitigations
- GitHub Actions uses `paths` filter to trigger only relevant pipelines
- Each service has its own `Dockerfile`, `pom.xml`/`go.mod`/`package.json`
- Skaffold per service for independent local development
