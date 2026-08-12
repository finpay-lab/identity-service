package com.finpay.identity.service.infrastructure.jpa;

import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter for {@link PrincipalRepository} (Rule 4: interface in domain, impl
 * here). Uses Spring Data {@code save()} merge semantics; the aggregate version
 * is copied onto the entity so {@code @Version} drives optimistic locking.
 */
@Repository
public class JpaPrincipalRepository implements PrincipalRepository {

    private final PrincipalJpaRepository jpaRepository;

    public JpaPrincipalRepository(PrincipalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Principal save(Principal principal) {
        jpaRepository.save(toEntity(principal));
        return principal;
    }

    @Override
    public Optional<Principal> findById(UUID principalId) {
        return jpaRepository.findById(principalId).map(this::toDomain);
    }

    @Override
    public Optional<Principal> findByIdentityProviderAndExternalSubject(String identityProvider, String externalSubject) {
        return jpaRepository.findByIdentityProviderAndExternalSubject(identityProvider, externalSubject)
                .map(this::toDomain);
    }

    private Principal toDomain(PrincipalEntity entity) {
        return Principal.hydrate(
                entity.getPrincipalId(),
                entity.getExternalSubject(),
                entity.getIdentityProvider(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getRoles(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getVersion());
    }

    private PrincipalEntity toEntity(Principal principal) {
        PrincipalEntity entity = new PrincipalEntity();
        entity.setPrincipalId(principal.principalId());
        entity.setExternalSubject(principal.externalSubject());
        entity.setIdentityProvider(principal.identityProvider());
        entity.setEmail(principal.email());
        entity.setDisplayName(principal.displayName());
        entity.setRoles(principal.roles());
        entity.setActive(principal.isActive());
        entity.setCreatedAt(principal.createdAt());
        entity.setVersion(principal.version());
        return entity;
    }
}