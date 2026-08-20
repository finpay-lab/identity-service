package com.finpay.identity.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KycJpaRepository extends JpaRepository<KycEntity, String> {}
