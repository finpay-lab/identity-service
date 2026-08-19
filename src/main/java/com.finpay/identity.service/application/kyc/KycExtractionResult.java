package com.finpay.identity.service.application.kyc;

import com.finpay.identity.service.domain.kyc.ExtractedKycFields;
import com.finpay.identity.service.domain.kyc.KycExtraction;
import com.finpay.identity.service.domain.kyc.KycState;

import java.util.UUID;

/** Result of a successful KYC extraction, returned to the web layer. */
public record KycExtractionResult(
        UUID extractionId,
        UUID customerId,
        KycState kycState,
        ExtractedKycFields fields) {

    public static KycExtractionResult from(KycExtraction extraction) {
        return new KycExtractionResult(extraction.id(), extraction.customerId(), extraction.state(), extraction.fields());
    }
}
