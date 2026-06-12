package com.fooddelivery.payment.domain.event;

import java.util.UUID;

public class PaymentFailedEvent {
    private UUID orderId;
    private UUID paymentId;
    private String failureReason;

    public PaymentFailedEvent() {}

    public PaymentFailedEvent(UUID orderId, UUID paymentId, String failureReason) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.failureReason = failureReason;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
