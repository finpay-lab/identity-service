package com.finpay.identity.service.infrastructure.outbox;

import com.finpay.identity.service.application.DomainEventPublisher;
import com.finpay.identity.service.domain.DomainEvent;
import com.finpay.identity.service.domain.IdentityVerified;
import com.finpay.identity.service.domain.OutboxMessage;
import com.finpay.identity.service.domain.OutboxRepository;
import com.finpay.identity.service.domain.RoleChanged;
import com.finpay.identity.service.infrastructure.kafka.IdentityEventEnvelope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbox-backed {@link DomainEventPublisher}: serializes each domain event to
 * the Kafka wire envelope and inserts an outbox row. Called inside the use-case
 * transaction, so rows commit atomically with the aggregate change (ADR-0004).
 */
@Component
public class OutboxEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(List<DomainEvent> domainEvents) {
        for (DomainEvent event : domainEvents) {
            outboxRepository.save(toOutboxMessage(event));
        }
    }

    private OutboxMessage toOutboxMessage(DomainEvent event) {
        try {
            return new OutboxMessage(
                    UUID.randomUUID(),
                    "Principal",
                    event.aggregateId(),
                    event.eventType(),
                    objectMapper.writeValueAsString(toEnvelope(event)),
                    event.eventId(),
                    false,
                    Instant.now());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event " + event, e);
        }
    }

    private IdentityEventEnvelope toEnvelope(DomainEvent event) {
        if (event instanceof IdentityVerified verified) {
            return new IdentityEventEnvelope(
                    verified.eventId().toString(),
                    verified.eventType(),
                    verified.occurredAt(),
                    1,
                    verified.principalId().toString(),
                    new IdentityEventEnvelope.IdentityVerifiedPayload(
                            verified.principalId(),
                            verified.externalSubject(),
                            verified.identityProvider(),
                            verified.email(),
                            verified.roles().stream().map(r -> r.name()).toList()));
        }
        if (event instanceof RoleChanged changed) {
            return new IdentityEventEnvelope(
                    changed.eventId().toString(),
                    changed.eventType(),
                    changed.occurredAt(),
                    1,
                    changed.principalId().toString(),
                    new IdentityEventEnvelope.RoleChangedPayload(
                            changed.principalId(),
                            changed.role().name(),
                            changed.granted()));
        }
        throw new IllegalStateException("Unsupported domain event type: " + event.getClass().getName());
    }
}