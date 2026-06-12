package com.fooddelivery.payment.application;

import com.fooddelivery.payment.domain.event.PaymentRefundedEvent;
import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.model.PaymentStatus;
import com.fooddelivery.payment.domain.model.Refund;
import com.fooddelivery.payment.domain.port.inbound.RefundPaymentUseCase;
import com.fooddelivery.payment.domain.port.outbound.EventPublisher;
import com.fooddelivery.payment.domain.port.outbound.PaymentRepository;
import com.fooddelivery.payment.exception.BusinessException;
import com.fooddelivery.payment.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RefundPaymentApplicationService implements RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    public RefundPaymentApplicationService(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void refundPayment(UUID paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment not found with id: " + paymentId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("ALREADY_REFUNDED", "Payment has already been refunded");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("INVALID_STATUS_FOR_REFUND", "Payment must be in SUCCESS state to be refunded");
        }

        BigDecimal refundAmount = amount != null ? amount : payment.getAmount();

        payment.markRefunded();
        paymentRepository.save(payment);

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setStatus("SUCCESS");
        refund.setCreatedAt(Instant.now());
        paymentRepository.saveRefund(refund);

        PaymentRefundedEvent refundedEvent = new PaymentRefundedEvent(
                payment.getOrderId(),
                payment.getId(),
                refundAmount
        );
        eventPublisher.publishRefunded(refundedEvent);
    }

    @Override
    public void refundPaymentByOrderId(UUID orderId, String reason) {
        // Saga rollback: if payment does not exist, ignore and return gracefully
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            return;
        }

        // Avoid duplicate refunds
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        // Only refund if successfully charged
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            // If it is in PENDING/PROCESSING/FAILED, just update state to FAILED or REFUNDED to release locks
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Cancelled by Saga: " + reason);
            paymentRepository.save(payment);
            return;
        }

        BigDecimal refundAmount = payment.getAmount();

        payment.markRefunded();
        paymentRepository.save(payment);

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(payment.getId());
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setStatus("SUCCESS");
        refund.setCreatedAt(Instant.now());
        paymentRepository.saveRefund(refund);

        PaymentRefundedEvent refundedEvent = new PaymentRefundedEvent(
                payment.getOrderId(),
                payment.getId(),
                refundAmount
        );
        eventPublisher.publishRefunded(refundedEvent);
    }
}
