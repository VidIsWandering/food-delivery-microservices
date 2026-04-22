# Contributing to Food Delivery Microservices

## Git Workflow (GitHub Flow)

We use a simplified GitHub Flow with `main` as the only long-lived branch.

```
main (production-ready, protected)
  └── feature/FD-123-add-order-api
  └── feature/FD-124-dispatch-matching
  └── fix/FD-130-payment-timeout
```

### Branch Naming

```
feature/FD-<number>-<short-description>
fix/FD-<number>-<short-description>
docs/<short-description>
chore/<short-description>
```

## Commit Convention (Conventional Commits)

Every commit message must follow this format:

```
<type>(<scope>): <description>

[optional body]
[optional footer]
```

### Types

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `refactor` | Code refactor (no behavior change) |
| `test` | Adding or fixing tests |
| `chore` | CI/CD, build, dependencies |
| `perf` | Performance improvement |

### Scopes

Use the service name as scope: `order`, `restaurant`, `user`, `payment`, `dispatch`, `notification`, `infra`, `ci`, `docs`.

### Examples

```
feat(order): add order cancellation endpoint
fix(dispatch): handle redis connection timeout gracefully
docs(arch): update saga sequence diagram
chore(ci): add trivy security scan to Java pipeline
test(payment): add refund domain service unit tests
refactor(user): extract JWT logic to dedicated service class
```

## Pull Request Guidelines

### Before Creating a PR

1. Ensure your branch is up to date with `main`
2. Run `make test svc=<your-service>` and verify all tests pass
3. Run `make lint svc=<your-service>` and fix any issues
4. Self-review your code diff

### PR Requirements

- **Title**: Use conventional commit format (e.g., `feat(order): add cancellation endpoint`)
- **Description**: Explain **What** changed and **Why**
- **Reviewer**: At least 1 approval required
- **CI**: All pipeline checks must pass (lint + test + build)
- **Merge Strategy**: Squash merge into `main`

### PR Description Template

```markdown
## What
Brief description of the change.

## Why
Motivation and context for this change.

## How
Technical approach taken.

## Testing
How was this tested? (unit tests, manual testing, etc.)

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated (if applicable)
- [ ] No breaking changes to API contracts
```

## Code Review Checklist

Reviewers should verify:

- [ ] Code follows the service's internal architecture pattern (Hexagonal / Layered)
- [ ] Structured JSON logging is used (not `System.out.println` or `fmt.Println`)
- [ ] Error handling follows conventions (see API Style Guide)
- [ ] New endpoints are documented in OpenAPI spec
- [ ] Kafka events follow CloudEvents format
- [ ] Unit tests cover business logic
- [ ] No hardcoded secrets or configuration values
