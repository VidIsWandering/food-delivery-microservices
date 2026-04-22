package com.fooddelivery.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration.
 * Topics are also defined in deployments/infrastructure/kafka/kafka-cluster.yaml (Strimzi).
 * This config ensures topics exist for local development (auto.create.topics.enable=false).
 */
@Configuration
public class KafkaTopicConfig {

    // User Service only publishes to user-events (if needed)
    // It mainly CONSUMES from order-events, payment-events for Saga participation

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name("user-events")
            .partitions(6)
            .replicas(1)
            .build();
    }
}
