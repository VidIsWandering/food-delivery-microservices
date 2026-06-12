package com.fooddelivery.payment.domain.port.outbound;

import com.fooddelivery.payment.domain.model.PaymentMethod;
import com.fooddelivery.payment.domain.model.PaymentResult;
import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {
    PaymentResult charge(UUID orderId, BigDecimal amount, PaymentMethod method);
}
