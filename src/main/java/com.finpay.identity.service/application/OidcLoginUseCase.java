package com.finpay.identity.service.application;

import com.finpay.identity.service.domain.IdentityClaims;
import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case: complete an OIDC login (ADR-0006). Parses the gateway-validated IdP
 * ID token, verifies the issuer policy, maps the external subject to the
 * internal principal (idempotent create-or-reuse by {@code issuer + subject}),
 * records {@code IdentityVerified} for the outbox and issues the FinPay
 * internal token. {@code issuer + subject} uniqueness is the DB backstop for
 * simultaneous duplicate logins.
 */
@Service
public class OidcLoginUseCase {

    private final OidcIdentityParser identityParser;
    private final PrincipalRepository principalRepository;
    private final DomainEventPublisher eventPublisher;
    private final PrincipalTokenIssuer tokenIssuer;
    private final String allowedIssuer;

    public OidcLoginUseCase(
            OidcIdentityParser identityParser,
            PrincipalRepository principalRepository,
            DomainEventPublisher eventPublisher,
            PrincipalTokenIssuer tokenIssuer,
            @Value("${finpay.identity.issuer}") String allowedIssuer) {
        this.identityParser = identityParser;
        this.principalRepository = principalRepository;
        this.eventPublisher = eventPublisher;
        this.tokenIssuer = tokenIssuer;
        this.allowedIssuer = allowedIssuer;
    }

    @Transactional
    public OidcLoginResult login(OidcLoginCommand command) {
        if (command == null || command.idToken() == null || command.idToken().isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }
        IdentityClaims claims = identityParser.parse(command.idToken());
        if (claims.issuer() == null || !claims.issuer().equals(allowedIssuer)) {
            throw new OidcTokenRejectedException(
                    "Token issuer '" + claims.issuer() + "' is not allowed (expected '" + allowedIssuer + "')");
        }
        var existing = principalRepository.findByIdentityProviderAndExternalSubject(
                claims.issuer(), claims.subject());
        if (existing.isPresent()) {
            Principal principal = existing.orElseThrow();
            // Identity re-verified on login: observable via the outbox, no state change.
            eventPublisher.publish(List.of(principal.verify()));
            return new OidcLoginResult(principal, tokenIssuer.issue(principal), false);
        }
        Principal principal = Principal.create(claims);
        Principal saved = principalRepository.save(principal);
        // Same transaction: aggregate + outbox rows commit together (ADR-0004).
        eventPublisher.publish(principal.pullDomainEvents());
        return new OidcLoginResult(saved, tokenIssuer.issue(saved), true);
    }
}