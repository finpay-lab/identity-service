package com.finpay.identity.service.infrastructure.kyc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.identity.service.domain.kyc.DocumentExtractionException;
import com.finpay.identity.service.domain.kyc.DocumentExtractor;
import com.finpay.identity.service.domain.kyc.ExtractedKycFields;
import com.finpay.identity.service.domain.kyc.KycDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Vision/LLM {@link DocumentExtractor} adapter (BYOK AI key from the secret
 * store via {@link FinpayAiProperties}).
 *
 * <p>PII discipline (SECURITY.md):
 * <ul>
 *   <li>Never log document content or extracted fields — only the content hash
 *       and document type are logged.</li>
 *   <li>Minimize what is sent to the model: the image plus a fixed extraction
 *       prompt; no other customer PII is included.</li>
 *   <li>On failure the document is NOT accepted; nothing is persisted.</li>
 * </ul>
 */
public class VisionLlmDocumentExtractor implements DocumentExtractor {

    private static final Logger log = LoggerFactory.getLogger(VisionLlmDocumentExtractor.class);

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter ALT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinpayAiProperties properties;
    private final RestClient restClient;

    public VisionLlmDocumentExtractor(FinpayAiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public ExtractedKycFields extract(KycDocument document) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new DocumentExtractionException("AI provider key is not configured (FINPAY_AI_API_KEY)");
        }
        log.info("Extracting KYC fields from {}", document.toLogSafeString());

        String raw;
        try {
            raw = sendRequest(buildRequestBody(document));
        } catch (RestClientException e) {
            throw new DocumentExtractionException("Vision/LLM extraction call failed", e);
        }
        return parseExtractedFields(raw);
    }

    private String sendRequest(String requestBody) {
        return restClient.post()
                .uri(properties.getEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), (request, response) -> {
                    throw new DocumentExtractionException(
                            "AI provider returned HTTP " + response.getStatusCode().value());
                })
                .body(String.class);
    }

    private String buildRequestBody(KycDocument document) {
        String base64 = Base64.getEncoder().encodeToString(document.content());
        String imageUrl = "data:%s;base64,%s".formatted(
                document.contentType() == null ? "application/octet-stream" : document.contentType(),
                base64);
        String prompt = """
                Extract the following fields from the identity document image:
                full name, date of birth (YYYY-MM-DD), document number, document type.
                Return ONLY a JSON object with keys: fullName, dateOfBirth, documentNumber, documentType.
                Do not retain or store the document. Redact all other personal information.
                """;
        try {
            return JSON.writeValueAsString(Map.of(
                    "model", properties.getModel(),
                    "max_tokens", 256,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", prompt),
                                    Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)))))));
        } catch (JsonProcessingException e) {
            throw new DocumentExtractionException("Failed to build AI request", e);
        }
    }

    /**
     * Tolerantly parses a provider response: either a plain JSON object with the
     * expected keys, or an OpenAI-style {@code choices[].message.content} JSON
     * string.
     */
    static ExtractedKycFields parseExtractedFields(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DocumentExtractionException("AI provider returned an empty response");
        }
        try {
            JsonNode fields = resolvePayload(JSON.readTree(raw));
            if (fields == null || !fields.isObject()) {
                throw new DocumentExtractionException("AI response missing expected JSON payload");
            }
            String documentType = text(fields, "documentType");
            String fullName = text(fields, "fullName");
            String documentNumber = text(fields, "documentNumber");
            if (fullName == null || documentNumber == null) {
                throw new DocumentExtractionException("AI response missing required KYC fields (fullName/documentNumber)");
            }
            return new ExtractedKycFields(documentType, fullName, parseDate(fields), documentNumber);
        } catch (DocumentExtractionException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new DocumentExtractionException("AI response was not valid JSON", e);
        }
    }

    private static JsonNode resolvePayload(JsonNode root) throws JsonProcessingException {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isTextual()) {
                return JSON.readTree(content.asText());
            }
        }
        return root;
    }

    private static LocalDate parseDate(JsonNode fields) {
        String text = text(fields, "dateOfBirth");
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            // try an alternate format below
        }
        try {
            return LocalDate.parse(text, ALT_DATE);
        } catch (DateTimeParseException e) {
            throw new DocumentExtractionException("AI returned an unparseable dateOfBirth");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}