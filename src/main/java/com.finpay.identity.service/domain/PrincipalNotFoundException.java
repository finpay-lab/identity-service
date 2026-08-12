package com.finpay.identity.service.domain;

/** Raised when no internal principal exists for the requested identifier. Maps to 404. */
public final class PrincipalNotFoundException extends RuntimeException {

    private final java.util.UUID principalId;

    public PrincipalNotFoundException(java.util.UUID principalId) {
        super("Principal not found: " + principalId);
        this.principalId = principalId;
    }

    public java.util.UUID principalId() {
        return principalId;
    }
}