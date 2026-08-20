package com.finpay.identity.service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KycVerificationTest {

    @Test
    void extractionMovesStateToReviewNotApproved() {
        KycVerification k = new KycVerification("cust-1");
        assertThat(k.state()).isEqualTo(KycVerification.State.PENDING);
        k.recordExtraction(new DocumentExtractor.KycFields("Jane Doe", "1990-01-01", "AB123456", null));
        // Compliance: extraction only sets REVIEW; human must approve.
        assertThat(k.state()).isEqualTo(KycVerification.State.REVIEW);
        assertThat(k.extracted().fullName()).isEqualTo("Jane Doe");
    }

    @Test
    void approveOnlyAllowedFromReview() {
        KycVerification k = new KycVerification("cust-2");
        k.recordExtraction(new DocumentExtractor.KycFields("John", "1985-05-05", "XY9", null));
        k.approve();
        assertThat(k.state()).isEqualTo(KycVerification.State.APPROVED);
        // re-approve after finalize is rejected (Rule 9)
        assertThatThrownBy(k::approve).isInstanceOf(IllegalStateException.class);
    }
}
