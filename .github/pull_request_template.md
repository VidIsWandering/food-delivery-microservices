## What
<!-- Brief description of the change -->


## Why
<!-- Motivation and context -->


## How
<!-- Technical approach taken -->


## Testing
<!-- How was this tested? -->
- [ ] Unit tests added/updated
- [ ] Integration tests with Testcontainers
- [ ] Manual testing via Skaffold

## Checklist
- [ ] Code follows the service's internal architecture pattern
- [ ] Structured JSON logging used (no println)
- [ ] Error responses follow [API Style Guide](docs/development/API_STYLE_GUIDE.md)
- [ ] OpenAPI spec updated (if API changed)
- [ ] Kafka event schema updated (if events changed)
- [ ] Flyway migration added (if DB schema changed)
- [ ] No hardcoded secrets or config values
