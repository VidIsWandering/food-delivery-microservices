package com.fooddelivery.payment.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentSuccessEvent {
    private UUID orderId;
    private UUID paymentId;
    private BigDecimal amount;
    private String transactionId;

    public PaymentSuccessEvent() {}

    public PaymentSuccessEvent(UUID orderId, UUID paymentId, BigDecimal amount, String transactionId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
