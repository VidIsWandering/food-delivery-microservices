package com.fooddelivery.order.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Value Object representing a single item in an order.
 */
public record OrderItem(
    UUID itemId,
    String name,
    int quantity,
    Money unitPrice
) {
    public Money total() {
        return unitPrice.multiply(quantity);
    }

    public static Money calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::total)
            .reduce(Money.zero("VND"), Money::add);
    }
}
