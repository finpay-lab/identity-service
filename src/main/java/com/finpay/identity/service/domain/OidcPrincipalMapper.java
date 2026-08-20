package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;

import java.util.List;
import java.util.Map;

/**
 * Maps an external OIDC principal (e.g. Keycloak) to FinPay's internal
 * User/Role model (FP-30 / ADR-0006). The gateway later validates the FinPay
 * internal token; Keycloak is only contacted here, at login.
 */
public final class OidcPrincipalMapper {

    private final String expectedIssuer;

    public OidcPrincipalMapper(String expectedIssuer) {
        this.expectedIssuer = expectedIssuer;
    }

    public record InternalUser(String subject, List<Role> roles) {}

    public InternalUser map(Map<String, Object> claims) {
        Object iss = claims.get("iss");
        if (expectedIssuer != null && !expectedIssuer.equals(iss)) {
            throw new IllegalArgumentException("unexpected OIDC issuer: " + iss);
        }
        String sub = String.valueOf(claims.getOrDefault("sub", ""));
        if (sub.isBlank()) throw new IllegalArgumentException("missing sub claim");
        return new InternalUser(sub, extractRoles(claims));
    }

    @SuppressWarnings("unchecked")
    private List<Role> extractRoles(Map<String, Object> claims) {
        Object r = claims.get("roles");
        if (r instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(Role::valueOf).toList();
        }
        Object realm = claims.get("realm_access");
        if (realm instanceof Map<?, ?> m) {
            Object rolesObj = m.get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream().map(String::valueOf).map(Role::valueOf).toList();
            }
        }
        return List.of(Role.CUSTOMER);
    }
}
