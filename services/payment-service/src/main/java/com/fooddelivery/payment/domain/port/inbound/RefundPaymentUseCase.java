package com.fooddelivery.payment.domain.port.inbound;

import java.math.BigDecimal;
import java.util.UUID;

public interface RefundPaymentUseCase {
    void refundPayment(UUID paymentId, BigDecimal amount, String reason);
    void refundPaymentByOrderId(UUID orderId, String reason);
}
