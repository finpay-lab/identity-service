package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.Principal;

/** Result of an OIDC login: the mapped principal plus the freshly issued internal token. */
public record OidcLoginResult(
        Principal principal,
        PrincipalTokenIssuer.IssuedToken issuedToken,
        boolean created
) {
}