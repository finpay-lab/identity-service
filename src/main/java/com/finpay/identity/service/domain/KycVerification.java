package com.finpay.identity.service.domain;

import java.time.Instant;

/**
 * KYC verification state for a customer (FP-63). Compliance-sensitive: an
 * extracted document can only move the state to REVIEW (human approval
 * required before APPROVED). Auto-APPROVE is intentionally forbidden.
 */
public class KycVerification {

    public enum State { PENDING, REVIEW, APPROVED, REJECTED }

    private final String customerId;
    private State state;
    private DocumentExtractor.KycFields extracted;
    private final Instant createdAt;
    private Instant reviewedAt;

    public KycVerification(String customerId) {
        this.customerId = customerId;
        this.state = State.PENDING;
        this.createdAt = Instant.now();
    }

    public String customerId() { return customerId; }
    public State state() { return state; }
    public DocumentExtractor.KycFields extracted() { return extracted; }
    public Instant createdAt() { return createdAt; }
    public Instant reviewedAt() { return reviewedAt; }

    /**
     * Record an extraction result. Per compliance, this only transitions the
     * state to REVIEW — never APPROVED. A human reviewer calls approve/reject.
     */
    public void recordExtraction(DocumentExtractor.KycFields fields) {
        if (state == State.APPROVED || state == State.REJECTED) {
            throw new IllegalStateException("KYC already finalized: " + state);
        }
        this.extracted = fields;
        this.state = State.REVIEW;
        this.reviewedAt = Instant.now();
    }

    /** Human reviewer approve (Rule 9: only from REVIEW). */
    public void approve() {
        if (state != State.REVIEW) throw new IllegalStateException("cannot approve from " + state);
        this.state = State.APPROVED;
    }

    /** Human reviewer reject (Rule 9: only from REVIEW). */
    public void reject(String reason) {
        if (state != State.REVIEW) throw new IllegalStateException("cannot reject from " + state);
        this.state = State.REJECTED;
    }
}
