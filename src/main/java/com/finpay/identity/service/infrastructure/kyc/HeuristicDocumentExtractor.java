package com.finpay.identity.service.infrastructure.kyc;

import com.finpay.identity.service.domain.DocumentExtractor;
import com.finpay.identity.service.domain.DocumentExtractor.KycFields;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic document extractor (FP-63). No external LLM required for the
 * default path; an optional BYOK vision/LLM extractor can be supplied via
 * {@link #withVision(VisionExtractor)}. Redacts nothing here, but the contract
 * guarantees callers never log document bytes (compliance).
 */
public final class HeuristicDocumentExtractor implements DocumentExtractor {

    // Very small, deliberately naive matchers — the lab demonstrates the
    // flow, not production OCR. A real impl plugs a vision model via withVision().
    private static final Pattern DOB = Pattern.compile(
            "(?:DOB|Date of Birth|Born)\\s*[:\\-]?\\s*(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCNO = Pattern.compile(
            "(?:Doc(?:ument)?\\s*(?:No|Number|#)|Passport No)\\s*[:\\-]?\\s*([A-Z0-9]{6,})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile(
            "(?:Name|Full Name)\\s*[:\\-]?\\s*([A-Z][a-z]+\\s+[A-Z][a-z]+)",
            Pattern.CASE_INSENSITIVE);

    private VisionExtractor vision = null;

    public HeuristicDocumentExtractor withVision(VisionExtractor vision) {
        this.vision = vision;
        return this;
    }

    @FunctionalInterface
    public interface VisionExtractor {
        KycFields extract(byte[] doc, String contentType);
    }

    @Override
    public KycFields extract(byte[] documentBytes, String contentType) {
        if (vision != null) {
            try {
                return vision.extract(documentBytes, contentType);
            } catch (RuntimeException ex) {
                // fall through to heuristic
            }
        }
        String text = new String(documentBytes, StandardCharsets.UTF_8);
        Matcher mName = NAME.matcher(text);
        Matcher mDob = DOB.matcher(text);
        Matcher mDoc = DOCNO.matcher(text);
        return new KycFields(
                mName.find() ? mName.group(1) : null,
                mDob.find() ? mDob.group(1) : null,
                mDoc.find() ? mDoc.group(1) : null,
                null);
    }
}
