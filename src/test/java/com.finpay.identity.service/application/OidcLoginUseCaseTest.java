package com.finpay.identity.service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finpay.common.security.Role;
import com.finpay.identity.service.domain.IdentityClaims;
import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalRepository;
import com.finpay.identity.service.application.PrincipalTokenIssuer.IssuedToken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class OidcLoginUseCaseTest {

    @Mock
    private OidcIdentityParser identityParser;

    @Mock
    private PrincipalRepository principalRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private PrincipalTokenIssuer tokenIssuer;

    @InjectMocks
    private OidcLoginUseCase useCase;

    private static final String ISSUER = "finpay-keycloak";
    private static final String SUBJECT = "external-user-1";
    private static final IssuedToken TOKEN = new IssuedToken("header.payload.sig", Instant.now().plusSeconds(900));

    private IdentityClaims claims() {
        return new IdentityClaims(SUBJECT, ISSUER, "alice@finpay.dev", "alice", Set.of(Role.ADMIN));
    }

    @Test
    void first_login_maps_principal_and_issues_token() {
        when(identityParser.parse("t0")).thenReturn(claims());
        when(principalRepository.findByIdentityProviderAndExternalSubject(ISSUER, SUBJECT))
                .thenReturn(Optional.empty());
        when(tokenIssuer.issue(any(Principal.class))).thenReturn(TOKEN);

        OidcLoginResult result = useCase.login(new OidcLoginCommand("t0"));

        assertThat(result.created()).isTrue();
        assertThat(result.principal().externalSubject()).isEqualTo(SUBJECT);
        assertThat(result.principal().roles()).contains(Role.ADMIN);
        assertThat(result.issuedToken()).isEqualTo(TOKEN);

        verify(principalRepository).save(any(Principal.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void repeated_login_reuses_existing_principal_and_re_verifies() {
        Principal existing = Principal.hydrate(
                UUID.randomUUID(), SUBJECT, ISSUER, "alice@finpay.dev", "alice", Set.of(Role.CUSTOMER),
                true, Instant.now(), 0L);
        when(identityParser.parse("t1")).thenReturn(claims());
        when(principalRepository.findByIdentityProviderAndExternalSubject(ISSUER, SUBJECT))
                .thenReturn(Optional.of(existing));
        when(tokenIssuer.issue(existing)).thenReturn(TOKEN);

        OidcLoginResult result = useCase.login(new OidcLoginCommand("t1"));

        assertThat(result.created()).isFalse();
        assertThat(result.principal()).isSameAs(existing);
        verify(principalRepository, never()).save(any());
    }

    @Test
    void token_from_untrusted_issuer_is_rejected() {
        when(identityParser.parse("bad")).thenReturn(
                new IdentityClaims(SUBJECT, "evil-issuer", null, null, Set.of()));

        assertThatThrownBy(() -> useCase.login(new OidcLoginCommand("bad")))
                .isInstanceOf(OidcTokenRejectedException.class);
        verifyNoInteractions(principalRepository, eventPublisher, tokenIssuer);
    }

    @Test
    void blank_or_null_command_is_rejected() {
        assertThatThrownBy(() -> useCase.login(new OidcLoginCommand(null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.login(new OidcLoginCommand("  ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.login(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_failure_propagates_as_rejected_token() {
        when(identityParser.parse("malformed")).thenThrow(new OidcTokenRejectedException("nope"));

        assertThatThrownBy(() -> useCase.login(new OidcLoginCommand("malformed")))
                .isInstanceOf(OidcTokenRejectedException.class);
    }
}