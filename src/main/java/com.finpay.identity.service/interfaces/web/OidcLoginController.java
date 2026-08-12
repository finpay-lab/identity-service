package com.finpay.identity.service.interfaces.web;

import com.finpay.identity.service.application.GrantRoleCommand;
import com.finpay.identity.service.application.GetPrincipalUseCase;
import com.finpay.identity.service.application.OidcLoginCommand;
import com.finpay.identity.service.application.OidcLoginResult;
import com.finpay.identity.service.application.OidcLoginUseCase;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST transport ↔ use case mapping only (Rule 3). Completes an OIDC login with
 * an IdP token already validated by the API Gateway (ADR-0006).
 */
@RestController
@RequestMapping("/api/v1/oidc")
public class OidcLoginController {

    private final OidcLoginUseCase oidcLoginUseCase;

    public OidcLoginController(OidcLoginUseCase oidcLoginUseCase) {
        this.oidcLoginUseCase = oidcLoginUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<OidcLoginResponse> login(@Valid @RequestBody OidcLoginRequest request) {
        OidcLoginResult result = oidcLoginUseCase.login(new OidcLoginCommand(request.idToken()));
        return ResponseEntity
                .status(result.created() ? 201 : 200)
                .body(OidcLoginResponse.from(result.principal(), result.issuedToken(), result.created()));
    }
}