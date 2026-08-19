package com.finpay.identity.service.domain.kyc;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for {@link KycExtraction}. Implementation lives in
 * {@code infrastructure} (eventually a per-service PostgreSQL schema, ADR-0005).
 */
public interface KycRepository {

    Optional<KycExtraction> findByCustomerId(UUID customerId);

    void save(KycExtraction extraction);
}
