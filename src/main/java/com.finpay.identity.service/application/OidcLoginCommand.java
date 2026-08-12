package com.finpay.identity.service.application;

/** Command: complete an OIDC login with a gateway-validated IdP ID token. */
public record OidcLoginCommand(String idToken) {
}