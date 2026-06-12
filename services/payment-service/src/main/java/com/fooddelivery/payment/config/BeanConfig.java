package com.fooddelivery.payment.config;

import com.fooddelivery.payment.application.ProcessPaymentApplicationService;
import com.fooddelivery.payment.application.RefundPaymentApplicationService;
import com.fooddelivery.payment.adapter.outbound.gateway.MockPaymentGateway;
import com.fooddelivery.payment.adapter.outbound.gateway.StripePaymentGateway;
import com.fooddelivery.payment.adapter.outbound.persistence.PaymentMapper;
import com.fooddelivery.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.fooddelivery.payment.domain.port.inbound.RefundPaymentUseCase;
import com.fooddelivery.payment.domain.port.outbound.EventPublisher;
import com.fooddelivery.payment.domain.port.outbound.PaymentGateway;
import com.fooddelivery.payment.domain.port.outbound.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public PaymentMapper paymentMapper() {
        return new PaymentMapper();
    }

    @Bean
    public PaymentGateway paymentGateway(
            @Value("${payment.gateway.mode:mock}") String gatewayMode,
            @Value("${payment.gateway.stripe-secret-key:}") String stripeSecretKey) {
        if ("stripe".equalsIgnoreCase(gatewayMode)) {
            return new StripePaymentGateway(stripeSecretKey);
        }
        return new MockPaymentGateway();
    }

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            EventPublisher eventPublisher) {
        return new ProcessPaymentApplicationService(paymentRepository, paymentGateway, eventPublisher);
    }

    @Bean
    public RefundPaymentUseCase refundPaymentUseCase(
            PaymentRepository paymentRepository,
            EventPublisher eventPublisher) {
        return new RefundPaymentApplicationService(paymentRepository, eventPublisher);
    }
}
