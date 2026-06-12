package com.fooddelivery.payment.domain.port.outbound;

import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.model.Refund;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByOrderId(UUID orderId);
    Payment save(Payment payment);
    Refund saveRefund(Refund refund);
    
    // Idempotency support
    boolean isEventProcessed(String eventId);
    void markEventProcessed(String eventId, String eventType);
}
