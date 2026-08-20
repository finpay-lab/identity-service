package com.finpay.identity.service.web;

import com.finpay.common.security.Role;
import com.finpay.identity.service.domain.InternalTokenIssuer;
import com.finpay.identity.service.domain.OidcPrincipalMapper;
import com.finpay.identity.service.domain.OidcPrincipalMapper.InternalUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** OIDC token exchange (FP-30 / ADR-0006). Maps external principal -> internal token. */
@RestController
@RequestMapping("/v1/auth")
public class OidcExchangeController {

    private final OidcPrincipalMapper mapper;
    private final InternalTokenIssuer issuer;

    public OidcExchangeController(OidcPrincipalMapper mapper, InternalTokenIssuer issuer) {
        this.mapper = mapper;
        this.issuer = issuer;
    }

    /** Exchange an external OIDC id-token (claims) for a FinPay internal token. */
    @PostMapping("/oidc/exchange")
    public ResponseEntity<TokenResponse> exchange(@RequestBody Map<String, Object> claims) {
        InternalUser user = mapper.map(claims);
        // In prod, validate the OIDC signature here (Keycloak JWKS). Lab trusts claims.
        String token = issuer.issue(user.subject(), user.roles());
        return ResponseEntity.ok(new TokenResponse(user.subject(), token, user.roles()));
    }

    public record TokenResponse(String subject, String internalToken, List<Role> roles) {}
}
