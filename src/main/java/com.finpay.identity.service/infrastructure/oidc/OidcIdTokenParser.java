package com.finpay.identity.service.infrastructure.oidc;

import com.finpay.common.security.Role;
import com.finpay.identity.service.application.OidcIdentityParser;
import com.finpay.identity.service.application.OidcTokenRejectedException;
import com.finpay.identity.service.domain.IdentityClaims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Extracts identity claims from the payload segment of an OIDC ID token. The
 * signature is NOT verified here — the API Gateway validates the JWT against
 * the IdP JWKS before forwarding (ADR-0006). Reading the unverified claims is
 * purely structural and has no cryptographic pitfalls.
 */
@Component
public class OidcIdTokenParser implements OidcIdentityParser {

    private static final Logger log = LoggerFactory.getLogger(OidcIdTokenParser.class);
    private static final int TOKEN_SEGMENTS = 3;

    private final ObjectMapper objectMapper;

    public OidcIdTokenParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public IdentityClaims parse(String idToken) {
        try {
            String[] segments = idToken.split("\\.");
            if (segments.length != TOKEN_SEGMENTS || segments[1].isBlank()) {
                throw new OidcTokenRejectedException("OIDC token is not a JWS with a payload segment");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(segments[1]);
            JsonNode claims = objectMapper.readTree(decoded);
            JsonNode subject = claims.get("sub");
            if (subject == null || subject.isNull() || subject.asText().isBlank()) {
                throw new OidcTokenRejectedException("OIDC token is missing the required 'sub' claim");
            }
            String issuer = textOrNull(claims, "iss");
            return new IdentityClaims(
                    subject.asText(),
                    issuer,
                    textOrNull(claims, "email"),
                    textOrNull(claims, "preferred_username"),
                    extractRealmRoles(claims));
        } catch (OidcTokenRejectedException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Rejected unparseable OIDC token", e);
            throw new OidcTokenRejectedException("OIDC token payload could not be decoded");
        }
    }

    /** Keycloak shape: realm roles live under {@code realm_access.roles[].} */
    private Set<Role> extractRealmRoles(JsonNode claims) {
        Set<Role> roles = new HashSet<>();
        JsonNode realmAccess = claims.get("realm_access");
        if (realmAccess != null && realmAccess.isObject()) {
            JsonNode rawRoles = realmAccess.get("roles");
            if (rawRoles != null && rawRoles.isArray()) {
                for (JsonNode raw : rawRoles) {
                    try {
                        roles.add(Role.valueOf(raw.asText().trim().toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown IdP role: not part of the internal RBAC model.
                    }
                }
            }
        }
        return Set.copyOf(roles);
    }

    private String textOrNull(JsonNode claims, String field) {
        JsonNode node = claims.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }
}