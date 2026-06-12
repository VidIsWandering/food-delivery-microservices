package com.fooddelivery.payment.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentRefundedEvent {
    private UUID orderId;
    private UUID paymentId;
    private BigDecimal refundAmount;

    public PaymentRefundedEvent() {}

    public PaymentRefundedEvent(UUID orderId, UUID paymentId, BigDecimal refundAmount) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.refundAmount = refundAmount;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}
