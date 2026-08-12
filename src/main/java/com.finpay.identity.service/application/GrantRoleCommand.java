package com.finpay.identity.service.application;

import com.finpay.common.security.Role;

import java.util.UUID;

/** Command: grant an internal role to a principal (idempotent by principal+role). */
public record GrantRoleCommand(UUID principalId, Role role) {
}