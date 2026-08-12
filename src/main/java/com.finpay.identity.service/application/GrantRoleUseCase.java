package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalNotFoundException;
import com.finpay.identity.service.domain.PrincipalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: grant an internal role. Idempotent by (principalId, roleName)
 * (Rule 6) — granting an already held role is a no-op that emits no event.
 * Aggregate + outbox rows commit together; nothing is published remotely here
 * (Rule 5).
 */
@Service
public class GrantRoleUseCase {

    private final PrincipalRepository principalRepository;
    private final DomainEventPublisher eventPublisher;

    public GrantRoleUseCase(PrincipalRepository principalRepository, DomainEventPublisher eventPublisher) {
        this.principalRepository = principalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Principal grant(GrantRoleCommand command) {
        if (command == null || command.principalId() == null) {
            throw new IllegalArgumentException("principalId is required");
        }
        Principal principal = principalRepository.findById(command.principalId())
                .orElseThrow(() -> new PrincipalNotFoundException(command.principalId()));
        principal.grantRole(command.role());
        Principal saved = principalRepository.save(principal);
        eventPublisher.publish(principal.pullDomainEvents());
        return saved;
    }
}