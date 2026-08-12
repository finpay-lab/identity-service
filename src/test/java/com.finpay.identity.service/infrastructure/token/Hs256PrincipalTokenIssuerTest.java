package com.finpay.identity.service.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finpay.common.security.Role;
import com.finpay.identity.service.application.PrincipalTokenIssuer.IssuedToken;
import com.finpay.identity.service.domain.Principal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

class Hs256PrincipalTokenIssuerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef"; // 32 bytes

    @Test
    void issues_compact_jws_with_principal_claims() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Hs256PrincipalTokenIssuer issuer = new Hs256PrincipalTokenIssuer(mapper, SECRET, 15);

        UUID principalId = UUID.randomUUID();
        Principal principal = Principal.hydrate(
                principalId, "ext-1", "finpay-keycloak", "a@b.dev", "ext-1",
                Set.of(Role.CUSTOMER), true, Instant.now(), 0L);

        IssuedToken issued = issuer.issue(principal);
        assertThat(issued.token()).isNotNull();

        String[] segments = issued.token().split("\\.");
        assertThat(segments).hasSize(3);

        String header = new String(Base64.getUrlDecoder().decode(segments[0]));
        JsonNode claims = mapper.readTree(Base64.getUrlDecoder().decode(segments[1]));

        assertThat(header).contains("\"HS256\"");
        assertThat(claims.get("sub").asText()).isEqualTo(principalId.toString());
        assertThat(claims.get("ext").asText()).isEqualTo("ext-1");
        assertThat(claims.get("iss").asText()).isEqualTo("finpay-identity");
        assertThat(claims.get("roles").get(0).asText()).isEqualTo("CUSTOMER");
        assertThat(claims.get("permissions").get(0).asText()).isEqualTo("ROLE_CUSTOMER");
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).isEqualTo(900);
        assertThat(issued.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void refuses_short_shared_secret() {
        assertThatThrownBy(() -> new Hs256PrincipalTokenIssuer(new ObjectMapper(), "too-short", 15))
                .isInstanceOf(IllegalStateException.class);
    }
}