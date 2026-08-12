package com.finpay.identity.service.interfaces.web;

import jakarta.validation.constraints.NotBlank;

/** POST /api/v1/oidc/login body: the gateway-validated IdP OIDC ID token. */
public record OidcLoginRequest(
        @NotBlank(message = "idToken is required") String idToken
) {
}