package com.fooddelivery.order.domain.model;

/**
 * Value Object representing a delivery address.
 */
public record DeliveryAddress(
    String addressLine,
    double lat,
    double lng
) {}
