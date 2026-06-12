package com.fooddelivery.payment.adapter.outbound.persistence;

import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.model.Refund;
import com.fooddelivery.payment.domain.port.outbound.PaymentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataRefundRepository refundRepository;
    private final SpringDataProcessedEventRepository processedEventRepository;
    private final PaymentMapper paymentMapper;

    public JpaPaymentRepository(SpringDataPaymentRepository paymentRepository,
                                SpringDataRefundRepository refundRepository,
                                SpringDataProcessedEventRepository processedEventRepository,
                                PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.processedEventRepository = processedEventRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id).map(paymentMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).map(paymentMapper::toDomain);
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentJpaEntity jpaEntity = paymentMapper.toJpa(payment);
        PaymentJpaEntity savedEntity = paymentRepository.save(jpaEntity);
        return paymentMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public Refund saveRefund(Refund refund) {
        RefundJpaEntity jpaEntity = paymentMapper.toJpa(refund);
        RefundJpaEntity savedEntity = refundRepository.save(jpaEntity);
        return paymentMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEventProcessed(String eventId) {
        if (eventId == null) {
            return false;
        }
        return processedEventRepository.existsById(eventId);
    }

    @Override
    @Transactional
    public void markEventProcessed(String eventId, String eventType) {
        if (eventId == null) {
            return;
        }
        ProcessedEventJpaEntity entity = new ProcessedEventJpaEntity(eventId, eventType, Instant.now());
        processedEventRepository.save(entity);
    }
}
