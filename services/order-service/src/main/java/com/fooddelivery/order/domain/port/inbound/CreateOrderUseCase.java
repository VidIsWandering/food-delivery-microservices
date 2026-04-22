package com.fooddelivery.order.domain.port.inbound;

import com.fooddelivery.order.domain.model.DeliveryAddress;
import com.fooddelivery.order.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port: Use case for creating a new order.
 * Implemented by Application layer.
 */
public interface CreateOrderUseCase {

    record CreateOrderCommand(
        UUID customerId,
        UUID restaurantId,
        List<OrderItemCommand> items,
        DeliveryAddress deliveryAddress,
        String specialInstructions
    ) {}

    record OrderItemCommand(UUID itemId, int quantity) {}

    UUID execute(CreateOrderCommand command);
}
