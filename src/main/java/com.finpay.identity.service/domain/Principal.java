package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Principal aggregate (ADR-0006): the internal FinPay user derived from a
 * verified external subject. Owns the external↔internal mapping plus its role
 * grants. Pure domain — no Spring/JPA/Kafka imports (Rule 4). Events are
 * collected after each change and pulled out by the use case, which persists
 * aggregate + outbox rows in one transaction (ADR-0004).
 */
public final class Principal {

    private final UUID principalId;
    private final String externalSubject;
    private final String identityProvider;
    private String email;
    private String displayName;
    private final Set<Role> roles = EnumSet.noneOf(Role.class);
    private boolean active;
    private final Instant createdAt;
    private long version;

    /** Domain events produced by the last change, not yet dispatched. */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Principal(UUID principalId, String externalSubject, String identityProvider,
                      String email, String displayName, Set<Role> roles, boolean active,
                      Instant createdAt, long version) {
        this.principalId = principalId;
        this.externalSubject = externalSubject;
        this.identityProvider = identityProvider;
        this.email = email;
        this.displayName = displayName;
        this.roles.addAll(roles);
        this.active = active;
        this.createdAt = createdAt;
        this.version = version;
    }

    /**
     * Maps a verified external identity to a new internal principal (ADR-0006).
     * Idempotent at the use-case level: the (identityProvider, externalSubject)
     * unique key prevents duplicates on concurrent login. Records
     * {@link IdentityVerified}.
     */
    public static Principal create(IdentityClaims claims) {
        if (claims == null || claims.subject() == null || claims.subject().isBlank()) {
            throw new IllegalArgumentException("OIDC subject is required to map an identity");
        }
        Set<Role> mappedRoles = mapExternalRoles(claims.externalRoles());
        String subject = claims.subject().trim();
        if (claims.preferredUsername() != null && !claims.preferredUsername().isBlank()) {
            subject = claims.preferredUsername().trim();
        }
        Instant now = Instant.now();
        Principal principal = new Principal(
                UUID.randomUUID(),
                claims.subject().trim(),
                claims.issuer(),
                claims.email(),
                subject,
                mappedRoles,
                true,
                now,
                0L);
        principal.verify();
        return principal;
    }

    /**
     * Records an {@link IdentityVerified} event for the current verified
     * identity. Called on first mapping (from {@link #create(IdentityClaims)})
     * and on every subsequent successful login, so each OIDC verification is
     * observable via the outbox.
     */
    public IdentityVerified verify() {
        IdentityVerified event = new IdentityVerified(
                UUID.randomUUID(),
                principalId,
                externalSubject,
                identityProvider,
                email,
                Set.copyOf(roles),
                Instant.now());
        domainEvents.add(event);
        return event;
    }

    /** Rebuilds an existing aggregate from persistence (no new event emitted). */
    public static Principal hydrate(UUID principalId, String externalSubject, String identityProvider,
                                    String email, String displayName, Set<Role> roles, boolean active,
                                    Instant createdAt, long version) {
        return new Principal(principalId, externalSubject, identityProvider, email, displayName,
                roles, active, createdAt, version);
    }

    /**
     * Grants a role to this principal. Idempotent (Rule 6): granting an already
     * held role is a no-op and emits no event. Returns the recorded
     * {@link RoleChanged} or {@code null} when the role was already present.
     */
    public RoleChanged grantRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (!roles.add(role)) {
            return null;
        }
        RoleChanged event = new RoleChanged(UUID.randomUUID(), principalId, role, true, Instant.now());
        domainEvents.add(event);
        return event;
    }

    /**
     * Revokes a role. Idempotent: revoking an absent role is a no-op and emits
     * no event. Returns the recorded {@link RoleChanged} or {@code null}.
     */
    public RoleChanged revokeRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (!roles.remove(role)) {
            return null;
        }
        RoleChanged event = new RoleChanged(UUID.randomUUID(), principalId, role, false, Instant.now());
        domainEvents.add(event);
        return event;
    }

    /** Returns the recorded events for this change and clears the queue. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    /**
     * Maps IdP role names to internal {@link Role} values, ignoring unknown
     * names (the IdP is a conformist boundary — ADR-0006). When the IdP grants
     * no known roles the principal defaults to {@link Role#CUSTOMER}.
     */
    private static Set<Role> mapExternalRoles(Set<Role> externalRoles) {
        EnumSet<Role> mapped = EnumSet.noneOf(Role.class);
        if (externalRoles != null) {
            for (Role role : externalRoles) {
                if (role != null) {
                    mapped.add(role);
                }
            }
        }
        if (mapped.isEmpty()) {
            mapped.add(Role.CUSTOMER);
        }
        return mapped;
    }

    public UUID principalId() {
        return principalId;
    }

    public String externalSubject() {
        return externalSubject;
    }

    public String identityProvider() {
        return identityProvider;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }

    public boolean isActive() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** Last-known optimistic-lock version (owned by persistence, mirrored here). */
    public long version() {
        return version;
    }

    /** @param role raw role name as supplied by the transport layer */
    public static Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
    }
}