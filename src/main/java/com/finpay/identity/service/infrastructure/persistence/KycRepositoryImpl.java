package com.finpay.identity.service.infrastructure.persistence;

import com.finpay.identity.service.domain.DocumentExtractor;
import com.finpay.identity.service.domain.KycVerification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class KycRepositoryImpl implements com.finpay.identity.service.domain.KycRepository {

    private final KycJpaRepository jpa;

    public KycRepositoryImpl(KycJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<KycVerification> find(String customerId) {
        return jpa.findById(customerId).map(KycEntity::toDomain);
    }

    @Override
    public void save(KycVerification k) {
        jpa.save(KycEntity.from(k));
    }
}
