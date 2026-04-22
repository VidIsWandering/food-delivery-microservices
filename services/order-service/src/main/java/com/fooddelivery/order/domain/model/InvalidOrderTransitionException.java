package com.fooddelivery.order.domain.model;

/**
 * Exception thrown when an invalid order state transition is attempted.
 */
public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
