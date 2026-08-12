package com.finpay.identity.service.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data access for {@link PrincipalEntity}. */
public interface PrincipalJpaRepository extends JpaRepository<PrincipalEntity, UUID> {

    Optional<PrincipalEntity> findByIdentityProviderAndExternalSubject(String identityProvider, String externalSubject);
}