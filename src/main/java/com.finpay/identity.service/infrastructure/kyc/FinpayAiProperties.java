package com.finpay.identity.service.infrastructure.kyc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the BYOK vision/LLM provider. The API key comes from a
 * secret store (wired via {@code FINPAY_AI_API_KEY} in the lab); it is never
 * logged or exposed in responses.
 */
@ConfigurationProperties(prefix = "finpay.ai")
public class FinpayAiProperties {

    private String endpoint;
    private String apiKey;
    private String model;
    private Duration timeout = Duration.ofSeconds(30);

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}