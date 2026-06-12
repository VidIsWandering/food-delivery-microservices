# Hướng dẫn Kiểm tra & Xác minh Hệ thống (User, Restaurant & Payment Services)

Hệ thống hiện tại đã hoàn thiện đầy đủ **User Service**, **Restaurant Service**, và **Payment Service** với các đặc tả logic nghiệp vụ nghiêm ngặt, cơ chế bảo mật xác thực header qua Gateway Kong, ánh xạ kiểu dữ liệu nâng cao, và giao tiếp bất đồng bộ qua Kafka.

---

## 1. Restaurant Service - Tổng quan Kiến trúc & Điểm Nổi bật

Dịch vụ **Restaurant Service** đã được cấu hình chạy trên cổng **8082** nội bộ và map ra cổng **8002** trên máy Host.

### Ánh xạ Kiểu Dữ liệu Nâng cao (Hypersistence Utils)
- **Mảng Cuisine (`cuisine_types`)**: Được ánh xạ bằng `@Type(StringArrayType.class)` trên trường `String[]` kết hợp với `columnDefinition = "_varchar"` để Hibernate schema validator kiểm duyệt chính xác cấu trúc mảng PostgreSQL.
- **Thời gian hoạt động (`operating_hours`)**: Được ánh xạ kiểu JSONB sang `Map<String, DayHours>` trong `Restaurant.java`.
- **Tùy chọn món ăn (`options`)**: Được ánh xạ kiểu JSONB sang `List<MenuItemOption>` trong `MenuItem.java`.

### Logic Tính Tiền Nội Bộ (validate-items)
Endpoint nội bộ `POST /api/internal/restaurants/{id}/validate-items` thực hiện tính toán giá trị đơn hàng thực tế như sau:
1. Xác minh món ăn (`item_id`) có tồn tại, thuộc về nhà hàng (`{id}`) và đang ở trạng thái còn hàng (`is_available = true`).
2. Với mỗi món ăn, duyệt qua danh sách `selected_options` do khách hàng chọn.
3. Tìm kiếm option tương ứng trong DB và lấy ra `price_modifier` (giá cộng thêm) của choice tương ứng, cộng dồn vào giá gốc của món ăn.
4. Tính toán tổng tiền `subtotal` bằng cách nhân đơn giá sau tùy chọn với số lượng `quantity`.

### Kafka Integration (CloudEvents)
- **PaymentEventConsumer**: Lắng nghe topic `payment-events`. Khi nhận được event `PaymentSuccess`, hệ thống tự động log thông tin chuẩn bị món ăn.
- **RestaurantEventProducer**: Khi chủ quán thực hiện các hành động:
  - Cập nhật trạng thái nhà hàng (`/status`).
  - Xác nhận đơn (`/orders/{orderId}/accept`).
  - Báo đơn hàng sẵn sàng (`/orders/{orderId}/ready`).
  Hệ thống sẽ publish các event tương ứng (`RestaurantStatusChanged`, `OrderAccepted`, `OrderReadyForPickup`) dưới định dạng **CloudEvents v1.0** chuẩn lên topic `restaurant-events`.

---

## 2. Payment Service - Kiến trúc Lục giác (Hexagonal Architecture)

Dịch vụ **Payment Service** được thiết kế dựa trên kiến trúc **Hexagonal (Ports & Adapters)** nghiêm ngặt:
- **Tầng Domain (`domain/`)**: Hoàn toàn là PURE JAVA. Không import bất kỳ thư viện nào của Spring Framework, Jakarta JPA hay Apache Kafka. Chứa các Entity (`Payment`, `Refund`), Enums (`PaymentMethod`, `PaymentStatus`), Events (`PaymentSuccessEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`), và các Interfaces định nghĩa Ports (`ProcessPaymentUseCase`, `RefundPaymentUseCase`, `PaymentRepository`, `PaymentGateway`, `EventPublisher`).
- **Tầng Application (`application/`)**: Chứa logic xử lý nghiệp vụ (`ProcessPaymentApplicationService`, `RefundPaymentApplicationService`). Các class này là các class Java thuần túy, **không sử dụng** bất kỳ annotation nào như `@Service`, `@Component`, hay `@Autowired` của Spring.
- **Tầng Adapters (`adapter/`)**: 
  - **Inbound Rest**: `PaymentController` cung cấp API công khai kiểm tra trạng thái thanh toán và hoàn tiền cho Admin.
  - **Inbound Kafka**: `OrderEventConsumer` (lắng nghe topic `order-events` xử lý `OrderCreated`) và `CompensationEventConsumer` (lắng nghe các topic `restaurant-events` và `delivery-events` để xử lý `OrderRejected` và `DispatchFailed` rollback tiền).
  - **Outbound Persistence**: `JpaPaymentRepository` ánh xạ domain models với `PaymentJpaEntity`, `RefundJpaEntity` và `ProcessedEventJpaEntity`.
  - **Outbound Gateway**: `MockPaymentGateway` (giả lập 90% thành công, 10% thiếu tiền) và `StripePaymentGateway` (tích hợp SDK Stripe thật).
  - **Outbound Messaging**: `KafkaEventPublisher` xuất bản các event dưới dạng CloudEvents chuẩn.
- **Tầng Configuration (`config/`)**:
  - `BeanConfig`: Khởi tạo thủ công các service của Application Layer thông qua từ khóa `new` và wire các port với adapter tương ứng. Cho phép chuyển đổi Gateway linh hoạt (`payment.gateway.mode=mock|stripe`).

### Quy tắc Idempotent Consumer (Chống Trùng Lặp Giao Dịch)
Để tránh xử lý trùng lặp giao dịch tài chính khi Kafka gửi lại tin nhắn:
1. Mỗi sự kiện nhận được từ Kafka có một `event_id` độc nhất.
2. Trước khi gọi Use Case, các Consumers sẽ kiểm tra xem `event_id` này đã tồn tại trong bảng `processed_events` chưa bằng cách gọi qua Port `PaymentRepository`.
3. Nếu đã được xử lý, consumer lập tức bỏ qua. Nếu chưa, consumer thực thi logic nghiệp vụ và ghi nhận `event_id` vào bảng `processed_events` trong cùng một transaction của Database.

---

## 3. Hướng dẫn Chạy & Kiểm tra API

### Port Ánh Xạ
- **User Service**: Host Port `8001` -> Container Port `8080`.
- **Restaurant Service**: Host Port `8002` -> Container Port `8080`.
- **Payment Service**: Host Port `8004` -> Container Port `8080`.

### Cổng API Gateway Kong
Tất cả các API công khai đều được đi qua Kong Gateway tại địa chỉ `http://localhost:8000`.

### Swagger UI & Tài liệu OpenAPI
Bạn có thể truy cập Swagger UI trực tiếp của từng service tại:
- **User Service**: [http://localhost:8001/swagger-ui/index.html](http://localhost:8001/swagger-ui/index.html)
- **Restaurant Service**: [http://localhost:8002/swagger-ui/index.html](http://localhost:8002/swagger-ui/index.html)
- **Payment Service**: [http://localhost:8004/swagger-ui/index.html](http://localhost:8004/swagger-ui/index.html)

> [!NOTE]
> Server URL trong tài liệu Swagger của cả ba dịch vụ đều được định cấu hình trỏ mặc định về Gateway Kong (`http://localhost:8000`).

---

## 4. Danh sách API của Payment Service

### API Công khai (Public APIs)

#### Lấy trạng thái thanh toán theo đơn hàng (Get Payment Status by Order ID)
- **Method / URL**: `GET http://localhost:8004/api/v1/payments/{orderId}`
- **Headers bắt buộc**:
  - `X-User-Id`: `<UUID>`
  - `X-User-Role`: `CUSTOMER` hoặc `ADMIN`
- **Phản hồi mẫu**:
```json
{
  "success": true,
  "data": {
    "id": "e0b045e0-1c9f-43b9-a292-cc20e9803b0d",
    "order_id": "4a7b74f3-8bde-47cc-ae9f-149bfe38a5b2",
    "customer_id": "b182d334-a82f-410a-8bf8-d51a82f00a34",
    "amount": 100000.00,
    "currency": "VND",
    "method": "CREDIT_CARD",
    "status": "SUCCESS",
    "provider_transaction_id": "mock_tx_7c9f8d7b3a9e4b",
    "failure_reason": null,
    "created_at": "2026-06-12T18:00:00Z",
    "updated_at": "2026-06-12T18:00:01Z"
  },
  "meta": {
    "timestamp": "2026-06-12T18:00:05Z"
  }
}
```

#### Admin hoàn tiền thủ công (Initiate Manual Refund)
- **Method / URL**: `POST http://localhost:8004/api/v1/payments/{id}/refund`
- **Headers bắt buộc**:
  - `X-User-Id`: `<UUID của admin>`
  - `X-User-Role`: `ADMIN`
- **Body (JSON)**:
```json
{
  "reason": "Khách hàng muốn hủy đơn",
  "amount": 100000.00
}
```
- **Phản hồi**:
```json
{
  "success": true,
  "data": null,
  "meta": {
    "timestamp": "2026-06-12T18:05:00Z"
  }
}
```
