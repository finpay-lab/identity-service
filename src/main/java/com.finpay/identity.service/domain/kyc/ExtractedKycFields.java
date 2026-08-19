package com.finpay.identity.service.domain.kyc;

import java.time.LocalDate;

/**
 * PII extracted from an identity document by the vision/LLM extractor. This is
 * sensitive data: it must never be logged, and it must not be sent beyond the
 * KYC review workflow (see SECURITY.md PII guidance).
 */
public record ExtractedKycFields(
        String documentType,
        String fullName,
        LocalDate dateOfBirth,
        String documentNumber) {
}
