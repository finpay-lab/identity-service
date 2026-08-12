package com.finpay.identity.service.interfaces.web;

import com.finpay.identity.service.application.PrincipalTokenIssuer;
import com.finpay.identity.service.domain.Principal;

import java.time.Instant;

/** Login response: the mapped internal principal plus the FinPay internal token. */
public record OidcLoginResponse(
        PrincipalResponse principal,
        String token,
        String tokenType,
        Instant expiresAt,
        boolean created
) {

    public static OidcLoginResponse from(Principal principal, PrincipalTokenIssuer.IssuedToken token, boolean created) {
        return new OidcLoginResponse(
                PrincipalResponse.from(principal),
                token.token(),
                "Bearer",
                token.expiresAt(),
                created);
    }
}