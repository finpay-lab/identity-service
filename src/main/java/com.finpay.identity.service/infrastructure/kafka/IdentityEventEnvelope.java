package com.finpay.identity.service.infrastructure.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire envelope published on the {@code finpay.identity} topic — shape follows
 * the platform event conventions (eventId, eventType, occurredAt, version,
 * partitionKey, payload).
 */
public record IdentityEventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        int version,
        String partitionKey,
        Object payload
) {

    public record IdentityVerifiedPayload(
            UUID principalId,
            String externalSubject,
            String identityProvider,
            String email,
            java.util.List<String> roles
    ) {
    }

    public record RoleChangedPayload(
            UUID principalId,
            String role,
            boolean granted
    ) {
    }
}