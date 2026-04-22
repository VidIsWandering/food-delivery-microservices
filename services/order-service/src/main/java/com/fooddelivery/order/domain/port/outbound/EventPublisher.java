package com.fooddelivery.order.domain.port.outbound;

/**
 * Outbound port: Event publisher interface.
 * Implemented by Kafka adapter.
 */
public interface EventPublisher {

    void publish(String topic, String key, Object event);
}
