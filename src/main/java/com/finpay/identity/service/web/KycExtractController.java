package com.finpay.identity.service.web;

import com.finpay.identity.service.domain.DocumentExtractor;
import com.finpay.identity.service.domain.KycRepository;
import com.finpay.identity.service.domain.KycVerification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** KYC document intake (FP-63 / AI-6). Extracts fields, sets state to REVIEW (never APPROVED). */
@RestController
@RequestMapping("/v1/kyc")
public class KycExtractController {

    private static final Logger log = LoggerFactory.getLogger(KycExtractController.class);

    private final DocumentExtractor extractor;
    private final KycRepository repository;

    public KycExtractController(DocumentExtractor extractor, KycRepository repository) {
        this.extractor = extractor;
        this.repository = repository;
    }

    @PostMapping("/extract")
    public ResponseEntity<KycStatus> extract(@RequestParam String customerId,
                                             @RequestParam("document") MultipartFile document) {
        try {
            byte[] bytes = document.getBytes();
            // Compliance: never log document content.
            DocumentExtractor.KycFields fields = extractor.extract(bytes, document.getContentType());
            KycVerification kyc = repository.find(customerId)
                    .orElseGet(() -> new KycVerification(customerId));
            kyc.recordExtraction(fields);  // -> REVIEW (human approval required)
            repository.save(kyc);
            log.info("KYC extraction recorded for {} -> state={}", customerId, kyc.state());
            return ResponseEntity.accepted().body(new KycStatus(customerId, kyc.state().name(), fields));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new KycStatus(customerId, "ERROR", null));
        }
    }

    public record KycStatus(String customerId, String state, DocumentExtractor.KycFields fields) {}
}
