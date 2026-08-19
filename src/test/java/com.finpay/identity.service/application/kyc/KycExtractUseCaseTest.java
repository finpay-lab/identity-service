package com.finpay.identity.service.application.kyc;

import com.finpay.identity.service.domain.kyc.DocumentExtractionException;
import com.finpay.identity.service.domain.kyc.DocumentExtractor;
import com.finpay.identity.service.domain.kyc.ExtractedKycFields;
import com.finpay.identity.service.domain.kyc.InvalidKycTransitionException;
import com.finpay.identity.service.domain.kyc.KycDocument;
import com.finpay.identity.service.domain.kyc.KycExtraction;
import com.finpay.identity.service.domain.kyc.KycRepository;
import com.finpay.identity.service.domain.kyc.KycState;
import com.finpay.identity.service.infrastructure.kyc.InMemoryKycRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KycExtractUseCaseTest {

    private DocumentExtractor extractor;
    private InMemoryKycRepository repository;
    private KycExtractUseCase useCase;

    @BeforeEach
    void setUp() {
        extractor = mock(DocumentExtractor.class);
        repository = new InMemoryKycRepository();
        useCase = new KycExtractUseCase(extractor, repository);
    }

    @Test
    void mock_vision_llm_extracts_fields_and_kyc_stays_in_review() {
        UUID customerId = UUID.randomUUID();
        when(extractor.extract(any())).thenReturn(new ExtractedKycFields(
                "PASSPORT", "Jane Doe", LocalDate.of(1990, 1, 1), "AB123456"));

        KycExtractionResult result = useCase.extract(customerId, sampleDocument(customerId));

        assertThat(result.kycState()).isEqualTo(KycState.REVIEW);
        assertThat(result.fields().fullName()).isEqualTo("Jane Doe");
        assertThat(result.fields().documentNumber()).isEqualTo("AB123456");
        assertThat(result.fields().dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));

        KycExtraction persisted = repository.findByCustomerId(customerId).orElseThrow();
        assertThat(persisted.state()).isEqualTo(KycState.REVIEW);
        assertThat(persisted.fields().fullName()).isEqualTo("Jane Doe");
    }

    @Test
    void re_extraction_of_record_in_review_updates_fields_and_stays_in_review() {
        UUID customerId = UUID.randomUUID();
        when(extractor.extract(any()))
                .thenReturn(new ExtractedKycFields("PASSPORT", "Jane Doe", LocalDate.of(1990, 1, 1), "AB123456"))
                .thenReturn(new ExtractedKycFields("PASSPORT", "Jane A. Doe", LocalDate.of(1990, 1, 1), "AB123456"));

        useCase.extract(customerId, sampleDocument(customerId));
        KycExtractionResult second = useCase.extract(customerId, sampleDocument(customerId));

        assertThat(second.kycState()).isEqualTo(KycState.REVIEW);
        assertThat(second.fields().fullName()).isEqualTo("Jane A. Doe");
        assertThat(repository.findByCustomerId(customerId).orElseThrow().fields().fullName()).isEqualTo("Jane A. Doe");
    }

    @Test
    void extraction_failure_is_propagated_and_nothing_is_persisted() {
        UUID customerId = UUID.randomUUID();
        when(extractor.extract(any())).thenThrow(new DocumentExtractionException("provider down"));

        assertThatThrownBy(() -> useCase.extract(customerId, sampleDocument(customerId)))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("provider down");

        assertThat(repository.findByCustomerId(customerId)).isEmpty();
    }

    @Test
    void approved_record_cannot_be_sent_back_to_review() {
        UUID customerId = UUID.randomUUID();
        KycExtraction approved = KycExtraction.pending(customerId);
        approved.submitForReview(new ExtractedKycFields("PASSPORT", "Jane Doe", LocalDate.of(1990, 1, 1), "AB123456"));
        approved.approve();
        repository.save(approved);
        when(extractor.extract(any())).thenReturn(new ExtractedKycFields(
                "PASSPORT", "Jane Doe", LocalDate.of(1990, 1, 1), "AB123456"));

        assertThatThrownBy(() -> useCase.extract(customerId, sampleDocument(customerId)))
                .isInstanceOf(InvalidKycTransitionException.class);
    }

    private KycDocument sampleDocument(UUID customerId) {
        return KycDocument.of(customerId, "passport.png", "image/png", new byte[]{1, 2, 3});
    }
}
