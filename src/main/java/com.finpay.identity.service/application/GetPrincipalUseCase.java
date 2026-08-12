package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalNotFoundException;
import com.finpay.identity.service.domain.PrincipalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Use case: resolve an internal principal by id (read side). */
@Service
public class GetPrincipalUseCase {

    private final PrincipalRepository principalRepository;

    public GetPrincipalUseCase(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
    }

    @Transactional(readOnly = true)
    public Principal get(UUID principalId) {
        if (principalId == null) {
            throw new IllegalArgumentException("principalId is required");
        }
        return principalRepository.findById(principalId)
                .orElseThrow(() -> new PrincipalNotFoundException(principalId));
    }
}