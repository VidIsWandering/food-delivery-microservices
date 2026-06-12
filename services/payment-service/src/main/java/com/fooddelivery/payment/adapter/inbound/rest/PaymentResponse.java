package com.fooddelivery.payment.adapter.inbound.rest;

import com.fooddelivery.payment.domain.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String providerTransactionId;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public PaymentResponse() {}

    public PaymentResponse(Payment payment) {
        if (payment != null) {
            this.id = payment.getId();
            this.orderId = payment.getOrderId();
            this.customerId = payment.getCustomerId();
            this.amount = payment.getAmount();
            this.currency = payment.getCurrency();
            this.method = payment.getMethod() != null ? payment.getMethod().name() : null;
            this.status = payment.getStatus() != null ? payment.getStatus().name() : null;
            this.providerTransactionId = payment.getProviderTransactionId();
            this.failureReason = payment.getFailureReason();
            this.createdAt = payment.getCreatedAt();
            this.updatedAt = payment.getUpdatedAt();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
