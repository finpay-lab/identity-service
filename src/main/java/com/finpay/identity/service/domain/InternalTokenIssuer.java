package com.finpay.identity.service.domain;

import com.finpay.common.security.Role;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Issues FinPay INTERNAL tokens after an external OIDC principal is mapped
 * (FP-30 / ADR-0006). Signs with HMAC-SHA256 using the internal secret. The
 * internal token carries the mapped subject + roles so downstream services
 * (gateway, ledger, transfer) can authorize without re-reaching Keycloak.
 *
 * Dependency-free (no Spring Security OAuth) to keep the build green.
 */
public final class InternalTokenIssuer {

    private final String issuer;
    private final byte[] secret;
    private final long ttlSeconds;

    public InternalTokenIssuer(String issuer, String secret, long ttlSeconds) {
        this.issuer = issuer;
        this.secret = (secret == null || secret.isBlank())
                ? "dev-internal-secret".getBytes(StandardCharsets.UTF_8) : secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String subject, List<Role> roles) {
        long now = System.currentTimeMillis() / 1000L;
        long exp = now + ttlSeconds;
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"iss\":\"" + issuer + "\",\"sub\":\"" + subject +
                "\",\"roles\":" + rolesJson(roles) + ",\"iat\":" + now + ",\"exp\":" + exp + "}");
        String signingInput = header + "." + payload;
        String sig = sign(signingInput, secret);
        return signingInput + "." + sig;
    }

    private static String rolesJson(List<Role> roles) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(roles.get(i).name()).append("\"");
        }
        return sb.append("]").toString();
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String input, byte[] secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sign failed", e);
        }
    }
}
