package com.fooddelivery.payment.adapter.inbound.rest;

import java.math.BigDecimal;

public record RefundRequest(
    String reason,
    BigDecimal amount
) {}
