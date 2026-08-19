package com.finpay.identity.service.infrastructure.kyc;

import com.finpay.identity.service.domain.kyc.DocumentExtractionException;
import com.finpay.identity.service.domain.kyc.DocumentExtractor;
import com.finpay.identity.service.domain.kyc.ExtractedKycFields;
import com.finpay.identity.service.domain.kyc.KycDocument;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisionLlmDocumentExtractorTest {

    @Test
    void parses_plain_json_response() {
        ExtractedKycFields fields = VisionLlmDocumentExtractor.parseExtractedFields(
                "{\"fullName\":\"Jane Doe\",\"dateOfBirth\":\"1990-01-01\",\"documentNumber\":\"AB123456\",\"documentType\":\"PASSPORT\"}");

        assertThat(fields.fullName()).isEqualTo("Jane Doe");
        assertThat(fields.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(fields.documentNumber()).isEqualTo("AB123456");
        assertThat(fields.documentType()).isEqualTo("PASSPORT");
    }

    @Test
    void parses_openai_style_choices_message_content() {
        String raw = "{\"choices\":[{\"message\":{\"content\":"
                + "{\"fullName\":\"John Smith\",\"dateOfBirth\":\"1985/05/20\",\"documentNumber\":\"XY987654\",\"documentType\":\"DRIVING_LICENSE\"}"
                + "}}]}";
        ExtractedKycFields fields = VisionLlmDocumentExtractor.parseExtractedFields(raw);

        assertThat(fields.fullName()).isEqualTo("John Smith");
        assertThat(fields.documentNumber()).isEqualTo("XY987654");
        assertThat(fields.documentType()).isEqualTo("DRIVING_LICENSE");
    }

    @Test
    void rejects_response_missing_required_fields() {
        assertThatThrownBy(() -> VisionLlmDocumentExtractor.parseExtractedFields("{\"documentType\":\"PASSPORT\"}"))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("missing required KYC fields");
    }

    @Test
    void rejects_non_json_response() {
        assertThatThrownBy(() -> VisionLlmDocumentExtractor.parseExtractedFields("<html>oops</html>"))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void missing_api_key_fails_fast_without_calling_the_provider() {
        FinpayAiProperties properties = properties(null);
        RestClient restClient = mock(RestClient.class);
        DocumentExtractor extractor = new VisionLlmDocumentExtractor(properties, restClient);

        assertThatThrownBy(() -> extractor.extract(document()))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("not configured");
        verify(restClient, never()).post();
    }

    @Test
    void provider_http_error_is_wrapped_as_extraction_failure() {
        FinpayAiProperties properties = properties("secret-key");
        RestClient restClient = mock(RestClient.class);

        RequestBodyUriSpec uriSpec = mock(RequestBodyUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.header(anyString(), anyString())).thenReturn(uriSpec);
        when(uriSpec.body(anyString())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(
                new RestClientResponseException("502 Bad Gateway", 502, "Bad Gateway", null, null, null));

        DocumentExtractor extractor = new VisionLlmDocumentExtractor(properties, restClient);

        assertThatThrownBy(() -> extractor.extract(document()))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("extraction call failed");
    }

    private FinpayAiProperties properties(String apiKey) {
        FinpayAiProperties properties = new FinpayAiProperties();
        properties.setEndpoint("https://ai.example.test/v1/responses");
        properties.setApiKey(apiKey);
        properties.setModel("vision-doc-extractor");
        return properties;
    }

    private KycDocument document() {
        return KycDocument.of(UUID.randomUUID(), "passport.png", "image/png", new byte[]{1, 2, 3});
    }
}
