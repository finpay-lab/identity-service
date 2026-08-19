package com.finpay.identity.service.domain.kyc;

/** Raised when a requested KYC state transition is not legal (rule: reject invalid transitions). */
public class InvalidKycTransitionException extends KycException {

    private final KycState from;
    private final KycState to;

    public InvalidKycTransitionException(KycState from, KycState to) {
        super("Illegal KYC state transition " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public KycState from() {
        return from;
    }

    public KycState to() {
        return to;
    }
}
