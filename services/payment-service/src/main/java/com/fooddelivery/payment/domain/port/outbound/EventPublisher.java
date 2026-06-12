package com.fooddelivery.payment.domain.port.outbound;

import com.fooddelivery.payment.domain.event.PaymentFailedEvent;
import com.fooddelivery.payment.domain.event.PaymentRefundedEvent;
import com.fooddelivery.payment.domain.event.PaymentSuccessEvent;

public interface EventPublisher {
    void publishSuccess(PaymentSuccessEvent event);
    void publishFailure(PaymentFailedEvent event);
    void publishRefunded(PaymentRefundedEvent event);
}
