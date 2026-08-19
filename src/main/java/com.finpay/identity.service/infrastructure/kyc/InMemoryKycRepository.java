package com.finpay.identity.service.infrastructure.kyc;

import com.finpay.identity.service.domain.kyc.KycExtraction;
import com.finpay.identity.service.domain.kyc.KycRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TEMPORARY in-memory store for the KYC intake slice. identity-service has no
 * persistence yet; the production implementation owns a PostgreSQL schema via
 * Flyway (ADR-0005). This keeps the domain repository seam honest while the
 * DB-backed slice lands. It never stores document content — only the extracted
 * fields the review workflow needs.
 */
public class InMemoryKycRepository implements KycRepository {

    private final Map<UUID, KycExtraction> store = new ConcurrentHashMap<>();

    @Override
    public Optional<KycExtraction> findByCustomerId(UUID customerId) {
        return Optional.ofNullable(store.get(customerId));
    }

    @Override
    public void save(KycExtraction extraction) {
        store.put(extraction.customerId(), extraction);
    }
}