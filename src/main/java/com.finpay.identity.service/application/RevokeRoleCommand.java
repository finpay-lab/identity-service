package com.finpay.identity.service.application;

import com.finpay.common.security.Role;

import java.util.UUID;

/** Command: revoke an internal role from a principal (idempotent by principal+role). */
public record RevokeRoleCommand(UUID principalId, Role role) {
}