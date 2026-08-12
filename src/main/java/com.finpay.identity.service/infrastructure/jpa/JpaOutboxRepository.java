package com.finpay.identity.service.infrastructure.jpa;

import com.finpay.identity.service.domain.OutboxMessage;
import com.finpay.identity.service.domain.OutboxRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** JPA adapter for {@link OutboxRepository} (ADR-0004). */
@Repository
public class JpaOutboxRepository implements OutboxRepository {

    private final OutboxJpaRepository jpaRepository;

    public JpaOutboxRepository(OutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(OutboxMessage message) {
        jpaRepository.save(toEntity(message));
    }

    @Override
    public List<OutboxMessage> findUnpublished(int limit) {
        return jpaRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void markPublished(UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setPublished(true);
            jpaRepository.save(entity);
        });
    }

    private OutboxMessage toDomain(OutboxMessageEntity entity) {
        return new OutboxMessage(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getEventId(),
                entity.isPublished(),
                entity.getCreatedAt());
    }

    private OutboxMessageEntity toEntity(OutboxMessage message) {
        OutboxMessageEntity entity = new OutboxMessageEntity();
        entity.setId(message.id());
        entity.setAggregateType(message.aggregateType());
        entity.setAggregateId(message.aggregateId());
        entity.setEventType(message.eventType());
        entity.setPayload(message.payload());
        entity.setEventId(message.eventId());
        entity.setPublished(message.published());
        entity.setCreatedAt(message.createdAt());
        return entity;
    }
}