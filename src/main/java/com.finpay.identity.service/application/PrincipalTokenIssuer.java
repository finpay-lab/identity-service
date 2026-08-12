package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.Principal;

import java.time.Instant;

/**
 * Issues the FinPay-internal principal token handed to the caller after a
 * successful OIDC login (ADR-0006: the IdP token itself is validated by the
 * gateway; this short-lived internal token carries the resolved principal,
 * roles and permissions for downstream services). Implementations live in
 * {@code infrastructure/}; the token is produced locally, never via a remote
 * call, so it is safe to create inside the use-case transaction (Rule 5).
 */
public interface PrincipalTokenIssuer {

    /** @return the signed internal token and its expiry. */
    IssuedToken issue(Principal principal);

    /** A minted internal token. */
    record IssuedToken(String token, Instant expiresAt) {
    }
}