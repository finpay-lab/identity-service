package com.finpay.identity.service.infrastructure.jpa;

import com.finpay.common.security.Role;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** JPA mapping for the {@code principals} + {@code principal_roles} tables (V1). */
@Entity
@Table(name = "principals")
public class PrincipalEntity {

    @Id
    @Column(name = "principal_id")
    private UUID principalId;

    @Column(name = "external_subject", nullable = false, length = 256)
    private String externalSubject;

    @Column(name = "identity_provider", nullable = false, length = 128)
    private String identityProvider;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "display_name", length = 256)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "principal_roles", joinColumns = @JoinColumn(name = "principal_id"))
    @Column(name = "role", nullable = false, length = 32)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Optimistic lock; incremented by Hibernate on every flush. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PrincipalEntity() {
        // JPA
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(UUID principalId) {
        this.principalId = principalId;
    }

    public String getExternalSubject() {
        return externalSubject;
    }

    public void setExternalSubject(String externalSubject) {
        this.externalSubject = externalSubject;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles == null ? EnumSet.noneOf(Role.class) : roles;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}