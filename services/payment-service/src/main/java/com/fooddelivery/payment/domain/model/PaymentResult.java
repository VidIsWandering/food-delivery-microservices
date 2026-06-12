package com.fooddelivery.payment.domain.model;

public record PaymentResult(
    boolean success,
    String transactionId,
    String failureReason
) {}
