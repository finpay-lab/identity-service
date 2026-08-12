package com.finpay.identity.service.interfaces.web;

import com.finpay.identity.service.application.GrantRoleCommand;
import com.finpay.identity.service.application.GrantRoleUseCase;
import com.finpay.identity.service.application.GetPrincipalUseCase;
import com.finpay.identity.service.application.RevokeRoleCommand;
import com.finpay.identity.service.application.RevokeRoleUseCase;
import com.finpay.identity.service.domain.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST transport ↔ use case mapping only (Rule 3). Principal lookup and
 * role-grant management; the current principal is provided by the gateway via a
 * trusted header after token validation (ADR-0006). Errors use common-web
 * problem details.
 */
@RestController
@RequestMapping("/api/v1")
public class PrincipalController {

    /** Header the gateway sets with the authenticated principal (ADR-0006). */
    public static final String PRINCIPAL_ID_HEADER = "X-FinPay-Principal-Id";

    private final GetPrincipalUseCase getPrincipalUseCase;
    private final GrantRoleUseCase grantRoleUseCase;
    private final RevokeRoleUseCase revokeRoleUseCase;

    public PrincipalController(
            GetPrincipalUseCase getPrincipalUseCase,
            GrantRoleUseCase grantRoleUseCase,
            RevokeRoleUseCase revokeRoleUseCase) {
        this.getPrincipalUseCase = getPrincipalUseCase;
        this.grantRoleUseCase = grantRoleUseCase;
        this.revokeRoleUseCase = revokeRoleUseCase;
    }

    @GetMapping("/principal")
    public PrincipalResponse getCurrentPrincipal(@RequestHeader(PRINCIPAL_ID_HEADER) UUID principalId) {
        return PrincipalResponse.from(getPrincipalUseCase.get(principalId));
    }

    @GetMapping("/principals/{principalId}")
    public PrincipalResponse getPrincipal(@PathVariable UUID principalId) {
        return PrincipalResponse.from(getPrincipalUseCase.get(principalId));
    }

    @GetMapping("/principals/{principalId}/roles")
    public List<PrincipalResponseRolesItem> listRoles(@PathVariable UUID principalId) {
        Principal principal = getPrincipalUseCase.get(principalId);
        return principal.roles().stream().sorted()
                .map(role -> new PrincipalResponseRolesItem(role.name(), role.toString()))
                .toList();
    }

    @PutMapping("/principals/{principalId}/roles/{roleName}")
    public ResponseEntity<Void> grantRole(@PathVariable UUID principalId, @PathVariable String roleName) {
        grantRoleUseCase.grant(new GrantRoleCommand(
                principalId, Principal.parseRole(roleName)));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/principals/{principalId}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable UUID principalId, @PathVariable String roleName) {
        revokeRoleUseCase.revoke(new RevokeRoleCommand(
                principalId, Principal.parseRole(roleName)));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /** Role representation per the OpenAPI {@code Role} schema. */
    public record PrincipalResponseRolesItem(String name, String description) {
    }
}