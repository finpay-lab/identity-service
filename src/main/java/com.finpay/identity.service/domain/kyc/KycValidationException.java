package com.finpay.identity.service.domain.kyc;

/** A KYC intake request that violates input invariants (empty/invalid document, ...). */
public class KycValidationException extends KycException {

    public KycValidationException(String message) {
        super(message);
    }

    public KycValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
