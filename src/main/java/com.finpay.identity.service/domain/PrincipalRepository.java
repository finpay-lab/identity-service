package com.finpay.identity.service.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for the principal aggregate — pure domain, implemented
 * in {@code infrastructure/} (Rule 4). Optimistic locking is delegated to the
 * persistence layer via the aggregate {@code version} field.
 */
public interface PrincipalRepository {

    /** Persists a new or updated aggregate, returning the authoritative state. */
    Principal save(Principal principal);

    Optional<Principal> findById(UUID principalId);

    /** Used by idempotent OIDC login (Rule 6): same IdP subject must map to the same principal. */
    Optional<Principal> findByIdentityProviderAndExternalSubject(String identityProvider, String externalSubject);
}