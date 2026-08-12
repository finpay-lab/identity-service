package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a role grant is added to or removed from a principal. Grants and
 * revokes are idempotent (Rule 6): only actual changes produce an event.
 */
public record RoleChanged(
        UUID eventId,
        UUID principalId,
        Role role,
        boolean granted,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "RoleChanged";
    }

    @Override
    public UUID aggregateId() {
        return principalId;
    }
}