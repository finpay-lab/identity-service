package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.IdentityClaims;

/**
 * Extracts {@link IdentityClaims} from an IdP-issued OIDC ID token. Signature
 * validation is (deliberately) not performed here — the API Gateway validates
 * the JWT against the IdP JWKS before forwarding (ADR-0006). Implementations
 * live in {@code infrastructure/} (Rule 4).
 */
public interface OidcIdentityParser {

    /**
     * Reads the unverified claims of a gateway-validated OIDC ID token.
     *
     * @throws OidcTokenRejectedException when the token is structurally invalid
     *                                    or is missing the required subject.
     */
    IdentityClaims parse(String idToken);
}