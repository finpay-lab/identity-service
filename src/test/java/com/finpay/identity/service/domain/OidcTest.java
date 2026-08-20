package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcPrincipalMapperTest {

    @Test
    void mapsExternalClaimsToInternalUserWithRoles() {
        OidcPrincipalMapper mapper = new OidcPrincipalMapper("https://kc/realms/finpay");
        Map<String, Object> claims = Map.of(
                "iss", "https://kc/realms/finpay",
                "sub", "user-123",
                "realm_access", Map.of("roles", List.of("CUSTOMER", "ADMIN")));
        OidcPrincipalMapper.InternalUser u = mapper.map(claims);
        assertThat(u.subject()).isEqualTo("user-123");
        assertThat(u.roles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.ADMIN);
    }

    @Test
    void rejectsUnexpectedIssuer() {
        OidcPrincipalMapper mapper = new OidcPrincipalMapper("https://kc/realms/finpay");
        Map<String, Object> claims = Map.of("iss", "evil", "sub", "x");
        assertThatThrownBy(() -> mapper.map(claims))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

class InternalTokenIssuerTest {

    @Test
    void issuesTokenCarryingSubjectAndRoles() {
        InternalTokenIssuer issuer = new InternalTokenIssuer("finpay-internal", "secret", 3600);
        String token = issuer.issue("u1", List.of(Role.CUSTOMER));
        // HMAC HS256 token has 3 dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
        // payload is base64url of a JSON containing sub and roles
        String payload = token.split("\\.")[1];
        String json = new String(java.util.Base64.getUrlDecoder().decode(payload));
        assertThat(json).contains("\"sub\":\"u1\"").contains("CUSTOMER");
    }
}
