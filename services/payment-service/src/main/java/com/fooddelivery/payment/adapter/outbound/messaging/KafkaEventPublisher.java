package com.fooddelivery.payment.adapter.outbound.messaging;

import com.fooddelivery.payment.domain.event.PaymentFailedEvent;
import com.fooddelivery.payment.domain.event.PaymentRefundedEvent;
import com.fooddelivery.payment.domain.event.PaymentSuccessEvent;
import com.fooddelivery.payment.domain.port.outbound.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "payment-events";
    private static final String SOURCE = "payment-service";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishSuccess(PaymentSuccessEvent event) {
        CloudEventEnvelope<PaymentSuccessData> envelope = new CloudEventEnvelope<>(
                UUID.randomUUID().toString(),
                SOURCE,
                "PaymentSuccess",
                Instant.now().toString(),
                "application/json",
                new PaymentSuccessData(
                        event.getOrderId().toString(),
                        event.getPaymentId().toString(),
                        event.getAmount().doubleValue(),
                        event.getTransactionId()
                )
        );
        send(event.getOrderId().toString(), envelope);
    }

    @Override
    public void publishFailure(PaymentFailedEvent event) {
        CloudEventEnvelope<PaymentFailedData> envelope = new CloudEventEnvelope<>(
                UUID.randomUUID().toString(),
                SOURCE,
                "PaymentFailed",
                Instant.now().toString(),
                "application/json",
                new PaymentFailedData(
                        event.getOrderId().toString(),
                        event.getPaymentId().toString(),
                        event.getFailureReason()
                )
        );
        send(event.getOrderId().toString(), envelope);
    }

    @Override
    public void publishRefunded(PaymentRefundedEvent event) {
        CloudEventEnvelope<PaymentRefundedData> envelope = new CloudEventEnvelope<>(
                UUID.randomUUID().toString(),
                SOURCE,
                "PaymentRefunded",
                Instant.now().toString(),
                "application/json",
                new PaymentRefundedData(
                        event.getOrderId().toString(),
                        event.getPaymentId().toString(),
                        event.getRefundAmount().doubleValue()
                )
        );
        send(event.getOrderId().toString(), envelope);
    }

    private void send(String key, Object envelope) {
        log.info("Publishing event key: {} to topic: {}", key, TOPIC);
        kafkaTemplate.send(TOPIC, key, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event key: {}", key, ex);
                    } else {
                        log.debug("Event successfully published to metadata: {}", result.getRecordMetadata());
                    }
                });
    }

    // --- CloudEvent Envelope Record ---
    public record CloudEventEnvelope<T>(
            String id,
            String source,
            String type,
            String time,
            String datacontenttype,
            T data
    ) {}

    // --- Inner Records matching Kafka Schema precisely ---
    public record PaymentSuccessData(
            String order_id,
            String payment_id,
            double amount,
            String transaction_id
    ) {}

    public record PaymentFailedData(
            String order_id,
            String payment_id,
            String failure_reason
    ) {}

    public record PaymentRefundedData(
            String order_id,
            String payment_id,
            double refund_amount
    ) {}
}
