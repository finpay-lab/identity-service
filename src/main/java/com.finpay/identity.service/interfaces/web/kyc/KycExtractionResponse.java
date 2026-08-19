package com.finpay.identity.service.interfaces.web.kyc;

import com.finpay.identity.service.application.kyc.KycExtractionResult;

import java.time.LocalDate;
import java.util.UUID;

/** Web DTO for a KYC extraction response. Intended for the human reviewer UI. */
public record KycExtractionResponse(
        UUID extractionId,
        UUID customerId,
        String kycState,
        ExtractedFields extractedFields,
        String message) {

    public static KycExtractionResponse from(KycExtractionResult result) {
        var f = result.fields();
        return new KycExtractionResponse(
                result.extractionId(),
                result.customerId(),
                result.kycState().name(),
                f == null ? null : new ExtractedFields(f.documentType(), f.fullName(), f.dateOfBirth(), f.documentNumber()),
                "Document accepted; KYC set to REVIEW pending manual verification");
    }

    /** Extracted PII returned to the reviewer (never logged server-side). */
    public record ExtractedFields(
            String documentType,
            String fullName,
            LocalDate dateOfBirth,
            String documentNumber) {
    }
}
