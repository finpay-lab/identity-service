package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;

import java.util.Set;

/**
 * External identity claims extracted from the IdP-issued OIDC ID token
 * (ADR-0006). The signature is validated by the API Gateway before the token
 * reaches this service, so this is a trusted, plain-data description of the
 * authenticated external subject. Kept dependency-free so the OIDC mapping
 * logic stays pure domain (Rule 4).
 *
 * @param subject          OIDC {@code sub} claim — stable per identity provider.
 * @param issuer           OIDC {@code iss} claim — the identity provider.
 * @param email            {@code email} claim (optional).
 * @param preferredUsername {@code preferred_username} claim (optional).
 * @param externalRoles    Realm/app roles granted by the IdP (optional).
 */
public record IdentityClaims(
        String subject,
        String issuer,
        String email,
        String preferredUsername,
        Set<Role> externalRoles
) {
}