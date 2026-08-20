package com.finpay.identity.service.infrastructure.persistence;

import com.finpay.identity.service.domain.DocumentExtractor;
import com.finpay.identity.service.domain.KycVerification;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "kyc_verifications")
public class KycEntity {

    @Id
    private String customerId;
    private String state;
    private String fullName;
    private String dateOfBirth;
    private String documentNumber;
    private String expiry;
    private Instant createdAt;
    private Instant reviewedAt;

    public KycEntity() {}

    public static KycEntity from(KycVerification k) {
        KycEntity e = new KycEntity();
        e.customerId = k.customerId();
        e.state = k.state().name();
        DocumentExtractor.KycFields f = k.extracted();
        if (f != null) {
            e.fullName = f.fullName();
            e.dateOfBirth = f.dateOfBirth();
            e.documentNumber = f.documentNumber();
            e.expiry = f.expiry();
        }
        e.createdAt = k.createdAt();
        e.reviewedAt = k.reviewedAt();
        return e;
    }

    public KycVerification toDomain() {
        KycVerification k = new KycVerification(customerId);
        // restore extracted fields + state without re-violating invariants
        DocumentExtractor.KycFields f = new DocumentExtractor.KycFields(fullName, dateOfBirth, documentNumber, expiry);
        if (state.equals("REVIEW")) k.recordExtraction(f);
        else if (state.equals("APPROVED")) { k.recordExtraction(f); k.approve(); }
        else if (state.equals("REJECTED")) { k.recordExtraction(f); k.reject("restored"); }
        return k;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String v) { this.dateOfBirth = v; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String v) { this.documentNumber = v; }
    public String getExpiry() { return expiry; }
    public void setExpiry(String v) { this.expiry = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant v) { this.reviewedAt = v; }
}
