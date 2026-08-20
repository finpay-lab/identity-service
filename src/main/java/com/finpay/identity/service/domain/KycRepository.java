package com.finpay.identity.service.domain;

import java.util.Optional;

/** Domain port for KYC persistence (Rule 4: no JPA imports). */
public interface KycRepository {
    Optional<KycVerification> find(String customerId);
    void save(KycVerification kyc);
}
