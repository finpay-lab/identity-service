package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalNotFoundException;
import com.finpay.identity.service.domain.PrincipalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: revoke an internal role. Idempotent (Rule 6) — revoking an absent
 * role is a no-op. Aggregate + outbox rows commit together (ADR-0004).
 */
@Service
public class RevokeRoleUseCase {

    private final PrincipalRepository principalRepository;
    private final DomainEventPublisher eventPublisher;

    public RevokeRoleUseCase(PrincipalRepository principalRepository, DomainEventPublisher eventPublisher) {
        this.principalRepository = principalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Principal revoke(RevokeRoleCommand command) {
        if (command == null || command.principalId() == null) {
            throw new IllegalArgumentException("principalId is required");
        }
        Principal principal = principalRepository.findById(command.principalId())
                .orElseThrow(() -> new PrincipalNotFoundException(command.principalId()));
        principal.revokeRole(command.role());
        Principal saved = principalRepository.save(principal);
        eventPublisher.publish(principal.pullDomainEvents());
        return saved;
    }
}