package com.finpay.identity.service.interfaces.web;

import com.finpay.common.security.Role;
import com.finpay.identity.service.domain.Principal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Principal read representation — matches the OpenAPI {@code Principal} schema. */
public record PrincipalResponse(
        UUID principalId,
        String externalSubject,
        String identityProvider,
        String email,
        String displayName,
        List<Role> roles,
        List<String> permissions,
        boolean active,
        Instant createdAt
) {

    public static PrincipalResponse from(Principal principal) {
        List<Role> roles = principal.roles().stream().sorted().toList();
        return new PrincipalResponse(
                principal.principalId(),
                principal.externalSubject(),
                principal.identityProvider(),
                principal.email(),
                principal.displayName(),
                roles,
                Role.authorities(roles),
                principal.isActive(),
                principal.createdAt());
    }
}