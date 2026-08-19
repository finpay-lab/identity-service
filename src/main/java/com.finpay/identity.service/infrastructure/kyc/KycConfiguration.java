package com.finpay.identity.service.infrastructure.kyc;

import com.finpay.identity.service.domain.kyc.DocumentExtractor;
import com.finpay.identity.service.domain.kyc.KycRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Wires the KYC intake infrastructure adapters. */
@Configuration
@EnableConfigurationProperties(FinpayAiProperties.class)
public class KycConfiguration {

    @Bean
    DocumentExtractor documentExtractor(FinpayAiProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.getTimeout());
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return new VisionLlmDocumentExtractor(properties, restClient);
    }

    @Bean
    KycRepository kycRepository() {
        return new InMemoryKycRepository();
    }
}