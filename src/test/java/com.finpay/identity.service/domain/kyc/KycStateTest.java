package com.finpay.identity.service.domain.kyc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KycStateTest {

    @Test
    void pending_allows_review_only() {
        assertThat(KycState.PENDING.canTransitionTo(KycState.REVIEW)).isTrue();
        assertThat(KycState.PENDING.canTransitionTo(KycState.APPROVED)).isFalse();
        assertThat(KycState.PENDING.canTransitionTo(KycState.REJECTED)).isFalse();
    }

    @Test
    void review_allows_approve_reject_and_re_extraction() {
        assertThat(KycState.REVIEW.canTransitionTo(KycState.APPROVED)).isTrue();
        assertThat(KycState.REVIEW.canTransitionTo(KycState.REJECTED)).isTrue();
        assertThat(KycState.REVIEW.canTransitionTo(KycState.REVIEW)).isTrue();
        assertThat(KycState.REVIEW.canTransitionTo(KycState.PENDING)).isFalse();
    }

    @Test
    void approve_and_reject_are_terminal() {
        assertThat(KycState.APPROVED.canTransitionTo(KycState.REVIEW)).isFalse();
        assertThat(KycState.APPROVED.canTransitionTo(KycState.REJECTED)).isFalse();
        assertThat(KycState.REJECTED.canTransitionTo(KycState.REVIEW)).isFalse();
        assertThat(KycState.REJECTED.canTransitionTo(KycState.APPROVED)).isFalse();
    }

    @Test
    void extraction_never_auto_approves() {
        // The extraction path may only enter REVIEW; APPROVE is a human decision.
        assertThat(KycState.PENDING.canTransitionTo(KycState.APPROVED)).isFalse();
    }
}
