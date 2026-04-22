# Java Services — Quick Start Guide

> Implementation guide for all Java-based microservices (User, Restaurant, Order, Payment).
> Read this before starting development.

## 1. Required Reading

| Document | Sections | Purpose |
|----------|----------|---------|
| [SADD.md](../architecture/SADD.md) | Part 1 (High-Level), Part 2 (Service Details) | Understand overall system design |
| [API_STYLE_GUIDE.md](API_STYLE_GUIDE.md) | **All sections** | Response format, error codes, naming conventions |
| [DATABASE_GUIDE.md](DATABASE_GUIDE.md) | **All sections** | Flyway, UUID, JSONB, naming conventions |
| [TESTING_STRATEGY.md](TESTING_STRATEGY.md) | Java sections | Testing pyramid, Testcontainers examples |
| [ADR-006](../architecture/adr/006-hexagonal-for-complex-services.md) | Full document | Which service uses which architecture pattern |

## 2. Architecture Patterns per Service

```
┌───────────────────────┬──────────────────────┐
│  Simplified Layered   │  Hexagonal (Ports    │
│                       │  & Adapters)         │
├───────────────────────┼──────────────────────┤
│  User Service         │  Order Service       │
│  Restaurant Service   │  Payment Service     │
└───────────────────────┴──────────────────────┘
```

### Layered Architecture (User, Restaurant)

```
controller/    → REST endpoints, receives request, returns ApiResponse
dto/           → Request/Response DTOs (@Valid annotations)
service/       → Business logic
repository/    → Spring Data JPA interfaces
model/         → JPA entities (@Entity)
exception/     → Domain exceptions (base classes provided)
config/        → Spring configuration (JacksonConfig, KafkaTopicConfig provided)
common/        → Shared classes (ApiResponse provided)
kafka/         → Kafka producers/consumers
```

### Hexagonal Architecture (Order, Payment)

```
domain/
├── model/     → Pure Java objects, MUST NOT import Spring/JPA
├── port/
│   ├── inbound/   → Use case interfaces (e.g., CreateOrderUseCase)
│   └── outbound/  → Repository/EventPublisher interfaces
├── event/     → Domain events
application/   → Use case implementations (orchestration)
adapter/
├── inbound/
│   ├── rest/  → Controllers, DTOs, ApiResponse, ExceptionHandler
│   └── kafka/ → Kafka event consumers
├── outbound/
│   ├── persistence/ → JPA repositories implementing domain ports
│   ├── messaging/   → Kafka publishers implementing EventPublisher port
│   └── rest/        → HTTP clients to other services
config/        → Spring configs
```

> **Hexagonal Rule:** The `domain/` package must be **pure Java**. Imports from `org.springframework.*`, `jakarta.persistence.*`, and `org.apache.kafka.*` are not allowed.

## 3. Pre-built Code (Ready to Use)

Each Java service includes the following files — **do not recreate them**:

| File | Function | Usage |
|------|----------|-------|
| `*ServiceApplication.java` | Spring Boot entry point | No modification needed |
| `common/ApiResponse.java` | Response wrapper | `return ResponseEntity.ok(ApiResponse.ok(data))` |
| `exception/GlobalExceptionHandler.java` | Centralized error handling | `throw new ResourceNotFoundException("USER_NOT_FOUND", "...")` |
| `exception/ResourceNotFoundException.java` | 404 errors | Extend for specific resources |
| `exception/DuplicateResourceException.java` | 409 errors | Extend for specific conflicts |
| `exception/BusinessException.java` | 422 errors | Extend for business rule violations |
| `config/JacksonConfig.java` | snake_case JSON output | Applied automatically |
| `config/KafkaTopicConfig.java` | Topic registration | Add new topics as needed |

## 4. Recommended Implementation Order

```
1. User Service         ← Simplest service, validates full development workflow
2. Restaurant Service   ← Introduces JSONB menu management
3. Order Service        ← Most complex: state machine, outbox, saga coordination
4. Payment Service      ← Mock gateway, compensating transactions
```

## 5. Endpoint Implementation Checklist

- [ ] Controller method returns `ApiResponse.ok(...)` or throws a domain exception
- [ ] Request DTO has `@Valid` + `@NotNull`/`@NotBlank` annotations
- [ ] Service method has unit tests
- [ ] Repository integration test uses Testcontainers (`extends BaseIntegrationTest`)
- [ ] Errors use domain exceptions (handled by `GlobalExceptionHandler`)
- [ ] JSON output is `snake_case` (enforced by `JacksonConfig`)
- [ ] Implementation matches the corresponding OpenAPI spec

## 6. Example: Implementing GET /api/v1/users/{id}

```java
// 1. Controller
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        UserResponse user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}

// 2. DTO (output uses snake_case automatically via JacksonConfig)
public record UserResponse(
    UUID id,
    String email,
    String fullName,  // → JSON: "full_name"
    String phone,
    String role,
    Instant createdAt  // → JSON: "created_at"
) {}

// 3. Service
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserResponse findById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "USER_NOT_FOUND",
                "User with id '" + id + "' not found"
            ));
        return toResponse(user);
    }
}

// 4. Expected JSON response:
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "full_name": "John Doe",
    "phone": "+84901234567",
    "role": "CUSTOMER",
    "created_at": "2026-04-22T03:00:00Z"
  },
  "meta": {
    "timestamp": "2026-04-22T03:30:00Z"
  }
}
```

## 7. API Contracts (Source of Truth)

Before implementing any endpoint, **read the corresponding OpenAPI spec**:

| Service | OpenAPI File |
|---------|-------------|
| User | `docs/api/user-service.openapi.yaml` |
| Restaurant | `docs/api/restaurant-service.openapi.yaml` |
| Order | `docs/api/order-service.openapi.yaml` |
| Payment | `docs/api/payment-service.openapi.yaml` |

## 8. Kafka Events (Source of Truth)

Before publishing or consuming events, **read**: `docs/api/events-schema.json`

All events must follow the CloudEvents format:
```json
{
  "id": "uuid",
  "type": "OrderCreated",
  "source": "order-service",
  "time": "2026-04-22T03:30:00Z",
  "data": { ... }
}
```

## 9. Local Development

```bash
# Start infrastructure (PostgreSQL, Kafka, Redis)
docker compose up -d

# Build service
cd services/user-service
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run tests
./mvnw test
```

Port mapping (local development):

| Service | Spring Port | Skaffold Forward |
|---------|------------|-----------------|
| user-service | 8080 | localhost:8001 |
| restaurant-service | 8080 | localhost:8002 |
| order-service | 8080 | localhost:8003 |
| payment-service | 8080 | localhost:8004 |
