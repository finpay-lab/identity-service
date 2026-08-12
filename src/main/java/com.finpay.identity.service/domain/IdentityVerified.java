package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Emitted whenever the OIDC identity of a caller is verified and mapped to an
 * internal principal (ADR-0006). Payload shape follows the platform event
 * conventions in {@code contracts/events/v1/}.
 */
public record IdentityVerified(
        UUID eventId,
        UUID principalId,
        String externalSubject,
        String identityProvider,
        String email,
        Set<Role> roles,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "IdentityVerified";
    }

    @Override
    public UUID aggregateId() {
        return principalId;
    }
}