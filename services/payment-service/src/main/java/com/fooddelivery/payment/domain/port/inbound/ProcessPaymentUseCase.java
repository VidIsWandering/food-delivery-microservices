package com.fooddelivery.payment.domain.port.inbound;

import com.fooddelivery.payment.domain.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessPaymentUseCase {
    void processPayment(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method);
}
