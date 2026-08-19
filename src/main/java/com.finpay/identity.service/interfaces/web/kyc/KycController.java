package com.finpay.identity.service.interfaces.web.kyc;

import com.finpay.identity.service.application.kyc.KycExtractUseCase;
import com.finpay.identity.service.domain.kyc.KycDocument;
import com.finpay.identity.service.domain.kyc.KycValidationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * KYC intake edge. Controllers map transport ↔ use case only — all business
 * rules live in the domain/application layers.
 */
@RestController
@RequestMapping("/kyc")
public class KycController {

    private final KycExtractUseCase kycExtractUseCase;

    public KycController(KycExtractUseCase kycExtractUseCase) {
        this.kycExtractUseCase = kycExtractUseCase;
    }

    /**
     * POST /kyc/extract — upload an identity document, extract KYC fields via
     * vision/LLM and move the customer's KYC to REVIEW (never auto-APPROVE).
     */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycExtractionResponse extract(
            @RequestParam("customerId") UUID customerId,
            @RequestParam("file") MultipartFile file) {
        return KycExtractionResponse.from(kycExtractUseCase.extract(customerId, toDocument(customerId, file)));
    }

    private KycDocument toDocument(UUID customerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KycValidationException("document file must not be empty");
        }
        try {
            return KycDocument.of(customerId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new KycValidationException("could not read uploaded document", e);
        }
    }
}
