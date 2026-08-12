package com.finpay.identity.service.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A domain event produced by the principal aggregate. Records are published to
 * Kafka via the transactional outbox (ADR-0004) and deduplicated downstream by
 * {@code eventId} (Rule 7).
 */
public interface DomainEvent {

    /** Globally unique event identifier; consumers MUST deduplicate on it. */
    UUID eventId();

    /** Business key of the principal the event refers to (partition key). */
    UUID aggregateId();

    /** Fixed event type discriminator, e.g. {@code IdentityVerified}. */
    String eventType();

    /** UTC timestamp of when the event occurred. */
    Instant occurredAt();
}