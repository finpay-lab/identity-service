package com.finpay.identity.service.application;

/** Raised when an OIDC ID token cannot be parsed or fails the issuer policy. Maps to 401. */
public final class OidcTokenRejectedException extends RuntimeException {

    public OidcTokenRejectedException(String message) {
        super(message);
    }
}