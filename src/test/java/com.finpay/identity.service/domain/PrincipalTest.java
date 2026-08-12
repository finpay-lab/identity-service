package com.finpay.identity.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finpay.common.security.Role;
import com.finpay.identity.service.domain.Principal;

import org.junit.jupiter.api.Test;

import java.util.Set;

class PrincipalTest {

    private static IdentityClaims claims(Role... roles) {
        return new IdentityClaims("sub-1", "finpay-keycloak", "alice@finpay.dev", "alice", Set.of(roles));
    }

    @Test
    void create_maps_external_identity_and_records_identity_verified() {
        Principal principal = Principal.create(claims(Role.ADMIN));

        assertThat(principal.principalId()).isNotNull();
        assertThat(principal.externalSubject()).isEqualTo("sub-1");
        assertThat(principal.identityProvider()).isEqualTo("finpay-keycloak");
        assertThat(principal.isActive()).isTrue();
        assertThat(principal.roles()).containsExactly(Role.ADMIN);

        assertThat(principal.pullDomainEvents()).singleElement().isInstanceOf(IdentityVerified.class);
    }

    @Test
    void create_defaults_to_customer_when_no_known_external_role() {
        Principal principal = Principal.create(claims());

        assertThat(principal.roles()).containsExactly(Role.CUSTOMER);
        principal.pullDomainEvents(); // IdentityVerified
    }

    @Test
    void create_requires_subject() {
        assertThatThrownBy(() -> Principal.create(new IdentityClaims(null, "finpay-keycloak", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void grant_role_is_idempotent_and_only_changes_emit_events() {
        Principal principal = Principal.create(claims());
        principal.pullDomainEvents(); // discard the IdentityVerified from create()

        RoleChanged granted = principal.grantRole(Role.ADMIN);
        assertThat(granted).isNotNull();
        assertThat(granted.role()).isEqualTo(Role.ADMIN);
        assertThat(granted.granted()).isTrue();
        assertThat(principal.pullDomainEvents()).singleElement().isInstanceOf(RoleChanged.class);

        // Granting again is a no-op: no event, same state.
        assertThat(principal.grantRole(Role.ADMIN)).isNull();
        assertThat(principal.pullDomainEvents()).isEmpty();
    }

    @Test
    void revoke_role_is_idempotent() {
        Principal principal = Principal.create(claims(Role.ADMIN));
        principal.pullDomainEvents();

        assertThat(principal.revokeRole(Role.ADMIN)).isNotNull();
        assertThat(principal.roles()).isEmpty();

        // Revoking an absent role is a no-op.
        assertThat(principal.revokeRole(Role.ADMIN)).isNull();
        assertThat(principal.pullDomainEvents()).hasSize(1);
    }

    @Test
    void grant_and_revoke_null_role_are_rejected() {
        Principal principal = Principal.create(claims());
        principal.pullDomainEvents();

        assertThatThrownBy(() -> principal.grantRole(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> principal.revokeRole(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_role_maps_known_and_rejects_unknown() {
        assertThat(Principal.parseRole("admin")).isEqualTo(Role.ADMIN);
        assertThat(Principal.parseRole("OPERATOR")).isEqualTo(Role.OPERATOR);
        assertThatThrownBy(() -> Principal.parseRole("SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Principal.parseRole(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_records_identity_verified_for_current_state() {
        Principal principal = Principal.create(claims());
        principal.pullDomainEvents();

        IdentityVerified verified = principal.verify();
        assertThat(verified.principalId()).isEqualTo(principal.principalId());
        assertThat(principal.pullDomainEvents()).singleElement().isSameAs(verified);
    }
}