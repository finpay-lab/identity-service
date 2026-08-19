package com.finpay.identity.service.domain.kyc;

/** Base type for domain errors raised by the KYC intake flow. */
public class KycException extends RuntimeException {

    public KycException(String message) {
        super(message);
    }

    public KycException(String message, Throwable cause) {
        super(message, cause);
    }
}
