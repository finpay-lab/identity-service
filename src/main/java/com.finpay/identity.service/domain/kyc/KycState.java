package com.finpay.identity.service.domain.kyc;

import java.util.Map;
import java.util.Set;

/**
 * KYC lifecycle state machine. Per AGENTS.md every state machine defines legal
 * transitions and rejects invalid ones. Automation (the vision/LLM extractor)
 * can only ever land a record in {@link #REVIEW} — {@link #APPROVED} is reserved
 * for a human compliance reviewer, never for machine extraction.
 */
public enum KycState {
    PENDING,
    REVIEW,
    APPROVED,
    REJECTED;

    private static final Map<KycState, Set<KycState>> LEGAL_TRANSITIONS = Map.of(
            PENDING, Set.of(REVIEW),
            REVIEW, Set.of(REVIEW, APPROVED, REJECTED));

    /** Whether a transition from {@code this} state to {@code target} is legal. */
    public boolean canTransitionTo(KycState target) {
        return LEGAL_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
