package com.finpay.identity.service.infrastructure.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data access for {@link OutboxMessageEntity}. */
public interface OutboxJpaRepository extends JpaRepository<OutboxMessageEntity, UUID> {

    List<OutboxMessageEntity> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
}