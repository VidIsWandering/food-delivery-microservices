package com.fooddelivery.payment.adapter.outbound.persistence;

import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.model.PaymentMethod;
import com.fooddelivery.payment.domain.model.PaymentStatus;
import com.fooddelivery.payment.domain.model.Refund;

public class PaymentMapper {

    public Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Payment domain = new Payment();
        domain.setId(entity.getId());
        domain.setOrderId(entity.getOrderId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setAmount(entity.getAmount());
        domain.setCurrency(entity.getCurrency());
        if (entity.getMethod() != null) {
            domain.setMethod(PaymentMethod.valueOf(entity.getMethod()));
        }
        if (entity.getStatus() != null) {
            domain.setStatus(PaymentStatus.valueOf(entity.getStatus()));
        }
        domain.setProviderTransactionId(entity.getProviderTransactionId());
        domain.setFailureReason(entity.getFailureReason());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public PaymentJpaEntity toJpa(Payment domain) {
        if (domain == null) {
            return null;
        }
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(domain.getId());
        entity.setOrderId(domain.getOrderId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(domain.getCurrency());
        if (domain.getMethod() != null) {
            entity.setMethod(domain.getMethod().name());
        }
        if (domain.getStatus() != null) {
            entity.setStatus(domain.getStatus().name());
        }
        entity.setProviderTransactionId(domain.getProviderTransactionId());
        entity.setFailureReason(domain.getFailureReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public Refund toDomain(RefundJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Refund domain = new Refund();
        domain.setId(entity.getId());
        domain.setPaymentId(entity.getPaymentId());
        domain.setAmount(entity.getAmount());
        domain.setReason(entity.getReason());
        domain.setStatus(entity.getStatus());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public RefundJpaEntity toJpa(Refund domain) {
        if (domain == null) {
            return null;
        }
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.setId(domain.getId());
        entity.setPaymentId(domain.getPaymentId());
        entity.setAmount(domain.getAmount());
        entity.setReason(domain.getReason());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
