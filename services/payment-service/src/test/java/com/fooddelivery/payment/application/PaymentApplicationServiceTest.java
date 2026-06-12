package com.fooddelivery.payment.application;

import com.fooddelivery.payment.domain.event.PaymentFailedEvent;
import com.fooddelivery.payment.domain.event.PaymentRefundedEvent;
import com.fooddelivery.payment.domain.event.PaymentSuccessEvent;
import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.model.PaymentMethod;
import com.fooddelivery.payment.domain.model.PaymentResult;
import com.fooddelivery.payment.domain.model.PaymentStatus;
import com.fooddelivery.payment.domain.model.Refund;
import com.fooddelivery.payment.domain.port.outbound.EventPublisher;
import com.fooddelivery.payment.domain.port.outbound.PaymentGateway;
import com.fooddelivery.payment.domain.port.outbound.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentApplicationServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentGateway paymentGateway;
    private EventPublisher eventPublisher;

    private ProcessPaymentApplicationService processPaymentService;
    private RefundPaymentApplicationService refundPaymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        eventPublisher = mock(EventPublisher.class);

        processPaymentService = new ProcessPaymentApplicationService(paymentRepository, paymentGateway, eventPublisher);
        refundPaymentService = new RefundPaymentApplicationService(paymentRepository, eventPublisher);
    }

    @Test
    void processPayment_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150000.00");
        PaymentMethod method = PaymentMethod.CREDIT_CARD;

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.charge(orderId, amount, method)).thenReturn(new PaymentResult(true, "tx_12345", null));

        processPaymentService.processPayment(orderId, customerId, amount, method);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
        assertEquals("tx_12345", savedPayment.getProviderTransactionId());
        assertNull(savedPayment.getFailureReason());

        verify(eventPublisher, times(1)).publishSuccess(any(PaymentSuccessEvent.class));
        verify(eventPublisher, never()).publishFailure(any());
    }

    @Test
    void processPayment_Failed() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150000.00");
        PaymentMethod method = PaymentMethod.CREDIT_CARD;

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.charge(orderId, amount, method)).thenReturn(new PaymentResult(false, null, "INSUFFICIENT_FUNDS"));

        processPaymentService.processPayment(orderId, customerId, amount, method);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());
        assertNull(savedPayment.getProviderTransactionId());
        assertEquals("INSUFFICIENT_FUNDS", savedPayment.getFailureReason());

        verify(eventPublisher, times(1)).publishFailure(any(PaymentFailedEvent.class));
        verify(eventPublisher, never()).publishSuccess(any());
    }

    @Test
    void refundPayment_Success() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment(paymentId, orderId, UUID.randomUUID(), new BigDecimal("100000.00"),
                "VND", PaymentMethod.CREDIT_CARD, PaymentStatus.SUCCESS, "tx_123", null, null, null);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refundPaymentService.refundPayment(paymentId, null, "Customer cancel");

        verify(paymentRepository, times(1)).save(payment);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());

        verify(paymentRepository, times(1)).saveRefund(any(Refund.class));
        verify(eventPublisher, times(1)).publishRefunded(any(PaymentRefundedEvent.class));
    }

    @Test
    void refundPaymentByOrderId_GracefulRollback() {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), orderId, UUID.randomUUID(), new BigDecimal("100000.00"),
                "VND", PaymentMethod.CREDIT_CARD, PaymentStatus.SUCCESS, "tx_123", null, null, null);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refundPaymentService.refundPaymentByOrderId(orderId, "Saga cancelled");

        verify(paymentRepository, times(1)).save(payment);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(paymentRepository, times(1)).saveRefund(any(Refund.class));
        verify(eventPublisher, times(1)).publishRefunded(any(PaymentRefundedEvent.class));
    }
}
