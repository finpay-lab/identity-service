package com.finpay.identity.service.infrastructure.config;

import com.finpay.identity.service.domain.DocumentExtractor;
import com.finpay.identity.service.domain.InternalTokenIssuer;
import com.finpay.identity.service.domain.OidcPrincipalMapper;
import com.finpay.identity.service.infrastructure.kyc.HeuristicDocumentExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityConfig {

    @Value("${finpay.identity.oidc-issuer:}")
    private String oidcIssuer;
    @Value("${finpay.identity.internal-token-secret:}")
    private String internalSecret;
    @Value("${finpay.identity.internal-token-issuer:finpay-internal}")
    private String internalIssuer;
    @Value("${finpay.identity.internal-token-ttl-seconds:3600}")
    private long internalTtl;

    @Bean
    public OidcPrincipalMapper oidcPrincipalMapper() {
        return new OidcPrincipalMapper(oidcIssuer);
    }

    @Bean
    public InternalTokenIssuer internalTokenIssuer() {
        return new InternalTokenIssuer(internalIssuer, internalSecret, internalTtl);
    }

    @Bean
    public DocumentExtractor documentExtractor() {
        return new HeuristicDocumentExtractor();
    }
}
