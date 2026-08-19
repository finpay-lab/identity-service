package com.finpay.identity.service.domain.kyc;

import java.util.Objects;
import java.util.UUID;

/**
 * KYC aggregate for one customer. Enforces legal state transitions (AGENTS.md
 * rule 9): extraction intake moves a record to {@link KycState#REVIEW} only;
 * APPROVED is a human-reviewer decision and never reached by automation.
 */
public class KycExtraction {

    private final UUID id;
    private final UUID customerId;
    private KycState state;
    private ExtractedKycFields fields;

    private KycExtraction(UUID id, UUID customerId, KycState state, ExtractedKycFields fields) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.state = Objects.requireNonNull(state, "state");
        this.fields = fields;
    }

    /** A customer with no prior record enters the flow in PENDING state. */
    public static KycExtraction pending(UUID customerId) {
        return new KycExtraction(UUID.randomUUID(), customerId, KycState.PENDING, null);
    }

    /**
     * Registers freshly extracted fields. Always lands in REVIEW — never
     * APPROVED. Legal from PENDING or REVIEW (re-extraction updates the record).
     */
    public void submitForReview(ExtractedKycFields extractedFields) {
        transition(KycState.REVIEW);
        this.fields = Objects.requireNonNull(extractedFields, "extractedFields");
    }

    /** Human reviewer approves. Legal only from REVIEW. */
    public void approve() {
        transition(KycState.APPROVED);
    }

    /** Human reviewer rejects. Legal only from REVIEW. */
    public void reject() {
        transition(KycState.REJECTED);
    }

    private void transition(KycState target) {
        if (!state.canTransitionTo(target)) {
            throw new InvalidKycTransitionException(state, target);
        }
        this.state = target;
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public KycState state() {
        return state;
    }

    public ExtractedKycFields fields() {
        return fields;
    }
}
