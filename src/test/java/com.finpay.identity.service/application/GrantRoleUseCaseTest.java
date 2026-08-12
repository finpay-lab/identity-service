package com.finpay.identity.service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.finpay.common.security.Role;
import com.finpay.identity.service.domain.Principal;
import com.finpay.identity.service.domain.PrincipalNotFoundException;
import com.finpay.identity.service.domain.PrincipalRepository;
import com.finpay.identity.service.domain.RoleChanged;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GrantRoleUseCaseTest {

    @Mock
    private PrincipalRepository principalRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private GrantRoleUseCase grantRoleUseCase;

    @InjectMocks
    private RevokeRoleUseCase revokeRoleUseCase;

    private static final UUID PRINCIPAL_ID = UUID.randomUUID();

    private Principal principal() {
        return Principal.hydrate(
                PRINCIPAL_ID, "ext-1", "finpay-keycloak", null, "ext-1",
                Set.of(Role.CUSTOMER), true, Instant.now(), 0L);
    }

    @Test
    void grant_persists_principal_and_emits_role_changed() {
        Principal existing = principal();
        when(principalRepository.findById(PRINCIPAL_ID)).thenReturn(Optional.of(existing));

        Principal saved = grantRoleUseCase.grant(new GrantRoleCommand(PRINCIPAL_ID, Role.ADMIN));

        assertThat(saved.roles()).contains(Role.ADMIN);
        verify(principalRepository).save(any(Principal.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<com.finpay.identity.service.domain.DomainEvent>> captor =
                ArgumentCaptor.forClass(java.util.List.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).singleElement().isInstanceOf(RoleChanged.class);
    }

    @Test
    void revoke_removes_role_and_emits_role_changed() {
        Principal existing = Principal.hydrate(
                PRINCIPAL_ID, "ext-1", "finpay-keycloak", null, "ext-1",
                Set.of(Role.CUSTOMER, Role.ADMIN), true, Instant.now(), 0L);
        when(principalRepository.findById(PRINCIPAL_ID)).thenReturn(Optional.of(existing));

        Principal saved = revokeRoleUseCase.revoke(new RevokeRoleCommand(PRINCIPAL_ID, Role.ADMIN));

        assertThat(saved.roles()).doesNotContain(Role.ADMIN);
        verify(principalRepository).save(any(Principal.class));
    }

    @Test
    void missing_principal_is_rejected() {
        when(principalRepository.findById(PRINCIPAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> grantRoleUseCase.grant(new GrantRoleCommand(PRINCIPAL_ID, Role.ADMIN)))
                .isInstanceOf(PrincipalNotFoundException.class);
        assertThatThrownBy(() -> revokeRoleUseCase.revoke(new RevokeRoleCommand(PRINCIPAL_ID, Role.ADMIN)))
                .isInstanceOf(PrincipalNotFoundException.class);
        verify(principalRepository, never()).save(any());
    }
}