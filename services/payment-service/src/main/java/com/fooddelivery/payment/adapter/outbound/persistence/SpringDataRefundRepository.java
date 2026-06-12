package com.fooddelivery.payment.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataRefundRepository extends JpaRepository<RefundJpaEntity, UUID> {
}
