# Database Guide

## 1. Database Strategy

| Service | Database | Schema/DB Name | Lý do |
|---------|----------|---------------|-------|
| User Service | PostgreSQL | `user_db` | Relational data, ACID for auth |
| Restaurant Service | PostgreSQL | `restaurant_db` | JSONB for flexible menu structure |
| Order Service | PostgreSQL | `order_db` | ACID transactions, Outbox table |
| Payment Service | PostgreSQL | `payment_db` | Financial data integrity |
| Dispatch Service | Redis | `db 0` | Geospatial indexing, ephemeral state |

> **Note:** Trong local/dev, chạy 1 PostgreSQL instance với multiple databases (logical separation). Production có thể tách thành instances riêng nếu cần.

## 2. Schema Migration (Flyway)

Mọi Java service **BẮT BUỘC** dùng [Flyway](https://flywaydb.org/) để quản lý schema changes.

### Migration File Convention

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_addresses_table.sql
├── V3__add_phone_column_to_users.sql
└── V4__create_driver_profiles_table.sql
```

### Naming Rules

```
V{version}__{description}.sql
```

- `V` prefix + version number (sequential)
- Double underscore `__` separator
- Description in `snake_case`
- **KHÔNG BAO GIỜ** sửa migration file đã apply. Tạo file mới để thay đổi.

### Flyway Configuration (Spring Boot)

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

## 3. Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Table names | snake_case, **plural** | `users`, `orders`, `menu_items` |
| Column names | snake_case | `created_at`, `total_amount`, `is_active` |
| Primary key | `id` (UUID) | `id UUID PRIMARY KEY` |
| Foreign key | `{referenced_table_singular}_id` | `user_id`, `order_id` |
| Boolean columns | `is_` or `has_` prefix | `is_active`, `has_verified` |
| Timestamp columns | `_at` suffix | `created_at`, `updated_at`, `delivered_at` |
| JSONB columns | Descriptive name | `options`, `items`, `operating_hours` |
| Indexes | `idx_{table}_{columns}` | `idx_orders_customer_id` |
| Unique constraints | `uq_{table}_{columns}` | `uq_users_email` |

## 4. PostgreSQL JSONB Usage

Restaurant Service sử dụng JSONB cho flexible menu data:

### Khi nào dùng JSONB vs Relational columns

| Dùng JSONB | Dùng Relational Column |
|------------|----------------------|
| Cấu trúc data thay đổi giữa các records (menu options) | Data cố định, query thường xuyên (name, price) |
| Không cần query trực tiếp trên nested fields | Cần JOIN, GROUP BY, ORDER BY |
| Schema-less / semi-structured data | Cần referential integrity (FK) |

### JSONB Query Examples

```sql
-- Tìm menu items có option "Size"
SELECT * FROM menu_items
WHERE options @> '[{"name": "Size"}]';

-- Lấy restaurant mở cửa thứ 2
SELECT * FROM restaurants
WHERE operating_hours->>'mon' IS NOT NULL;
```

### JPA Mapping (Hibernate + vladmihalcea types)

```java
@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    private UUID id;

    private String name;
    private BigDecimal price;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<MenuOption> options;  // Auto-serialized to JSONB
}
```

## 5. UUID Strategy

- **Tất cả** primary keys dùng UUID v4 (`gen_random_uuid()` trong PostgreSQL)
- Generate UUID ở **database level**, không ở application level
- Lý do: Tránh collision trong distributed system, không leak thông tin tuần tự

## 6. Soft Delete vs Hard Delete

| Resource | Strategy | Lý do |
|----------|----------|-------|
| Users | **Soft delete** (`is_active = false`) | Regulatory, audit trail |
| Orders | **Never delete** | Financial records |
| Payments | **Never delete** | Financial records |
| Menu Items | **Soft delete** (`is_available = false`) | Giữ lại cho order history reference |
| Driver Locations | **Hard delete** (Redis TTL) | Ephemeral data |

## 7. Connection Pool Configuration

```yaml
# Spring Boot - HikariCP defaults
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # Adjust based on pod count
      minimum-idle: 5
      connection-timeout: 30000   # 30s
      idle-timeout: 600000        # 10min
      max-lifetime: 1800000       # 30min
```
