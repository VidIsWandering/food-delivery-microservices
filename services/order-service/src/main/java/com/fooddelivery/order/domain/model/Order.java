package com.fooddelivery.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order Aggregate Root.
 * Contains all business rules for order lifecycle management.
 *
 * IMPORTANT: This class MUST NOT import any Spring, JPA, or Kafka classes.
 * It is pure Java domain logic.
 */
public class Order {

    private UUID id;
    private UUID customerId;
    private UUID restaurantId;
    private List<OrderItem> items;
    private DeliveryAddress deliveryAddress;
    private Money subtotal;
    private Money deliveryFee;
    private Money totalAmount;
    private OrderStatus status;
    private UUID driverId;
    private String specialInstructions;
    private Instant createdAt;
    private Instant updatedAt;

    // Private constructor - use factory method
    private Order() {}

    /**
     * Factory method to create a new order.
     */
    public static Order create(
            UUID customerId,
            UUID restaurantId,
            List<OrderItem> items,
            DeliveryAddress deliveryAddress,
            String specialInstructions) {

        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.restaurantId = restaurantId;
        order.items = List.copyOf(items);
        order.deliveryAddress = deliveryAddress;
        order.specialInstructions = specialInstructions;
        order.status = OrderStatus.CREATED;
        order.subtotal = OrderItem.calculateSubtotal(items);
        order.deliveryFee = Money.of(BigDecimal.valueOf(15000), "VND"); // TODO: calculate based on distance
        order.totalAmount = order.subtotal.add(order.deliveryFee);
        order.createdAt = Instant.now();
        order.updatedAt = Instant.now();
        return order;
    }

    /**
     * Transition order to a new status with validation.
     * @throws InvalidOrderTransitionException if the transition is not allowed
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidOrderTransitionException(
                String.format("Cannot transition from %s to %s", status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Assign a driver to this order.
     */
    public void assignDriver(UUID driverId) {
        if (this.status != OrderStatus.READY_FOR_PICKUP) {
            throw new InvalidOrderTransitionException(
                "Can only assign driver when order is READY_FOR_PICKUP");
        }
        this.driverId = driverId;
        transitionTo(OrderStatus.DRIVER_ASSIGNED);
    }

    /**
     * Cancel the order with a reason.
     */
    public void cancel() {
        if (!status.isCancellable()) {
            throw new InvalidOrderTransitionException(
                String.format("Cannot cancel order in %s status", status));
        }
        transitionTo(OrderStatus.CANCELLED);
    }

    // Getters (no setters - immutable where possible)
    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getRestaurantId() { return restaurantId; }
    public List<OrderItem> getItems() { return items; }
    public DeliveryAddress getDeliveryAddress() { return deliveryAddress; }
    public Money getSubtotal() { return subtotal; }
    public Money getDeliveryFee() { return deliveryFee; }
    public Money getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public UUID getDriverId() { return driverId; }
    public String getSpecialInstructions() { return specialInstructions; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
