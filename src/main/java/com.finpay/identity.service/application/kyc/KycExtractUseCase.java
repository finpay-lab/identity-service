package com.finpay.identity.service.application.kyc;

import com.finpay.identity.service.domain.kyc.DocumentExtractor;
import com.finpay.identity.service.domain.kyc.ExtractedKycFields;
import com.finpay.identity.service.domain.kyc.KycDocument;
import com.finpay.identity.service.domain.kyc.KycExtraction;
import com.finpay.identity.service.domain.kyc.KycRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case: ingest an identity document and move the customer's KYC to REVIEW.
 *
 * <p>Architecture notes:
 * <ul>
 *   <li>The vision/LLM call is a remote dependency — it runs <b>before</b> any
 *       transaction and never inside one (AGENTS.md rule 5). Persist+commit
 *       happens only after extraction succeeds.</li>
 *   <li>Extraction never auto-approves: the state machine only allows a move to
 *       REVIEW; a human compliance reviewer owns APPROVE/REJECT.</li>
 * </ul>
 */
@Service
public class KycExtractUseCase {

    private final DocumentExtractor documentExtractor;
    private final KycRepository kycRepository;

    public KycExtractUseCase(DocumentExtractor documentExtractor, KycRepository kycRepository) {
        this.documentExtractor = documentExtractor;
        this.kycRepository = kycRepository;
    }

    public KycExtractionResult extract(UUID customerId, KycDocument document) {
        // Remote call outside any transaction (rule 5).
        ExtractedKycFields fields = documentExtractor.extract(document);

        KycExtraction extraction = kycRepository.findByCustomerId(customerId)
                .orElseGet(() -> KycExtraction.pending(customerId));
        extraction.submitForReview(fields);
        kycRepository.save(extraction);

        return KycExtractionResult.from(extraction);
    }
}
