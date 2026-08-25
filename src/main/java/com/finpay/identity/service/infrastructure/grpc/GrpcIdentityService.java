package com.finpay.identity.service.infrastructure.grpc;

import com.finpay.identity.v1.GetPrincipalRequest;
import com.finpay.identity.v1.GrantRoleRequest;
import com.finpay.identity.v1.IdentityServiceGrpc;
import com.finpay.identity.v1.ListRolesRequest;
import com.finpay.identity.v1.Principal;
import com.finpay.identity.v1.RevokeRoleRequest;
import com.finpay.identity.v1.RevokeRoleResponse;
import com.finpay.identity.v1.Role;
import com.finpay.identity.v1.RoleList;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC server for the internal identity API (ADR-0014). Terminates internal
 * service-to-service calls over gRPC on port 9091. Backs the {@code identity}
 * contract defined in {@code platform/grpc-contracts}.
 *
 * <p>Role state is kept in an in-memory map for the lab; a production build would
 * back this with the Kyc/principal persistence layer. The gRPC surface is the
 * source of truth for how other services authorize (e.g. customer-service
 * resolves principal roles via this endpoint).
 */
@Service
public class GrpcIdentityService extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final Map<String, Set<String>> rolesByPrincipal = new ConcurrentHashMap<>();

    @Override
    public void getPrincipal(GetPrincipalRequest request, StreamObserver<Principal> response) {
        Principal principal = Principal.newBuilder()
                .setPrincipalId(request.getPrincipalId())
                .setName(request.getPrincipalId())
                .build();
        response.onNext(principal);
        response.onCompleted();
    }

    @Override
    public void listRoles(ListRolesRequest request, StreamObserver<RoleList> response) {
        RoleList.Builder list = RoleList.newBuilder();
        rolesByPrincipal.getOrDefault(request.getPrincipalId(), Set.of())
                .forEach(r -> list.addRoles(Role.newBuilder()
                        .setPrincipalId(request.getPrincipalId())
                        .setRoleName(r)
                        .build()));
        response.onNext(list.build());
        response.onCompleted();
    }

    @Override
    public void grantRole(GrantRoleRequest request, StreamObserver<Role> response) {
        rolesByPrincipal
                .computeIfAbsent(request.getPrincipalId(), k -> ConcurrentHashMap.newKeySet())
                .add(request.getRoleName());
        response.onNext(Role.newBuilder()
                .setPrincipalId(request.getPrincipalId())
                .setRoleName(request.getRoleName())
                .build());
        response.onCompleted();
    }

    @Override
    public void revokeRole(RevokeRoleRequest request, StreamObserver<RevokeRoleResponse> response) {
        Set<String> roles = rolesByPrincipal.get(request.getPrincipalId());
        boolean removed = roles != null && roles.remove(request.getRoleName());
        response.onNext(RevokeRoleResponse.newBuilder().setOk(removed).build());
        response.onCompleted();
    }
}
