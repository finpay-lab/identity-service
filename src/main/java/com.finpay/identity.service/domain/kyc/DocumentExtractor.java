package com.finpay.identity.service.domain.kyc;

/**
 * Port for the vision/LLM document extractor. The implementation lives in
 * {@code infrastructure} (BYOK AI key from the secret store, never logged
 * PII); the domain only depends on this interface.
 */
public interface DocumentExtractor {

    /**
     * Extracts KYC fields from an identity document. Never auto-approves:
     * extracted fields always feed a manual REVIEW step.
     *
     * @throws DocumentExtractionException if the provider fails or returns unusable output
     */
    ExtractedKycFields extract(KycDocument document);
}
