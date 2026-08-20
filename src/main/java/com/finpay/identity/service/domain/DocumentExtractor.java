package com.finpay.identity.service.domain;

import java.util.Map;

/**
 * Extracts KYC fields from an uploaded identity document (FP-63 / AI-6).
 * Domain contract; vision/LLM implementations live in {@code infrastructure/}.
 * BYOK LLM is supplied via {@link #withExtractor(VisionExtractor)} (optional).
 */
public interface DocumentExtractor {

    /** Extracted KYC fields (name, dob, documentNumber, expiry). */
    record KycFields(String fullName, String dateOfBirth, String documentNumber, String expiry) {}

    KycFields extract(byte[] documentBytes, String contentType);
}
