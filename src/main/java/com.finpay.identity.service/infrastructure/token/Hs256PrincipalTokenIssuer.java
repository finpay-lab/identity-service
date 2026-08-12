package com.finpay.identity.service.infrastructure.token;

import com.finpay.common.security.Role;
import com.finpay.identity.service.application.PrincipalTokenIssuer;
import com.finpay.identity.service.domain.Principal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Issues the FinPay-internal principal token as a compact JWS (HS256). This is
 * the short-lived internal token established in ADR-0006 (the gateway-validated
 * IdP token itself is never re-issued here). Derives its {@code roles} and
 * {@code permissions} claims from the principal so downstream services can
 * authorize without querying identity-service. The gateway verifies this token
 * on the next hop (FP-3).
 *
 * <p>Implements the JWS compact serialization directly on the JDK {@link Mac}
 * (no crypto dependency) and is covered by unit tests. The HS256 secret is
 * provided via {@code finpay.identity.token.secret} and must be at least 32
 * bytes — fail fast at construction when it is not.
 */
@Component
public class Hs256PrincipalTokenIssuer implements PrincipalTokenIssuer {

    private static final Logger log = LoggerFactory.getLogger(Hs256PrincipalTokenIssuer.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long ttlSeconds;

    public Hs256PrincipalTokenIssuer(
            ObjectMapper objectMapper,
            @Value("${finpay.identity.token.secret}") String secret,
            @Value("${finpay.identity.token.ttl-minutes:15}") long ttlMinutes) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlMinutes * 60L;
        if (this.secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "finpay.identity.token.secret must be at least 32 bytes for HS256");
        }
    }

    @Override
    public IssuedToken issue(Principal principal) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(ttlSeconds);
            String header = base64Url(json(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = base64Url(json(Map.of(
                    "iss", "finpay-identity",
                    "sub", principal.principalId().toString(),
                    "ext", principal.externalSubject(),
                    "roles", principal.roles().stream().map(Role::name).toList(),
                    "permissions", Role.authorities(principal.roles()),
                    "iat", now.getEpochSecond(),
                    "exp", expiresAt.getEpochSecond(),
                    "jti", UUID.randomUUID().toString())));
            String signingInput = header + "." + payload;
            String signature = base64Url(hmacSha256(signingInput));
            return new IssuedToken(signingInput + "." + signature, expiresAt);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize token claims", e);
        }
    }

    private String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private byte[] hmacSha256(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(input.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 unavailable", e);
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String base64Url(String json) {
        return base64Url(json.getBytes(StandardCharsets.UTF_8));
    }
}