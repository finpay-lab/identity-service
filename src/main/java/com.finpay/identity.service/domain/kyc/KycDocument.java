package com.finpay.identity.service.domain.kyc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * An uploaded identity document waiting for KYC extraction. Only the hash is
 * ever persisted/logged — the raw content must not be retained beyond the
 * extraction call (PII minimization, SECURITY.md).
 */
public record KycDocument(
        UUID customerId,
        String fileName,
        String contentType,
        byte[] content,
        String contentSha256) {

    /** Hard ceiling for a single upload, independently of servlet multipart limits. */
    public static final int MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;

    public KycDocument {
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(content, "document content is required");
        if (content.length == 0) {
            throw new KycValidationException("document content must not be empty");
        }
        if (content.length > MAX_DOCUMENT_BYTES) {
            throw new KycValidationException("document exceeds the 10 MB size limit");
        }
    }

    public static KycDocument of(UUID customerId, String fileName, String contentType, byte[] content) {
        return new KycDocument(customerId, fileName, contentType, content, sha256(content));
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Redacted description safe to log: hash + type + size, never content.
     */
    public String toLogSafeString() {
        return "KycDocument[sha256=" + contentSha256
                + ", contentType=" + contentType
                + ", size=" + content.length + " bytes]";
    }
}
