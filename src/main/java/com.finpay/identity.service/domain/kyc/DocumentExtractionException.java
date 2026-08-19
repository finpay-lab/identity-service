package com.finpay.identity.service.domain.kyc;

/** Raised when the vision/LLM document extractor fails or returns unusable output. */
public class DocumentExtractionException extends KycException {

    public DocumentExtractionException(String message) {
        super(message);
    }

    public DocumentExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
