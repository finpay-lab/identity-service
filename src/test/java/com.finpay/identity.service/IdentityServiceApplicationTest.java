package com.finpay.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.finpay.common.web.error.ErrorCode;
import org.junit.jupiter.api.Test;

class IdentityServiceApplicationTest {
    @Test
    void context_loads_placeholder_and_common_web_resolves() {
        // Proves the build compiles and com.finpay:common-web is on the classpath.
        assertThat(IdentityServiceApplication.class).isNotNull();
        assertThat(ErrorCode.class).isNotNull();
    }
}
