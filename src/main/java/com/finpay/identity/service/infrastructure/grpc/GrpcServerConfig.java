package com.finpay.identity.service.infrastructure.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Boots the internal gRPC server (ADR-0014) on a dedicated port, separate from
 * the REST port (8080) used for inbound/client traffic. The gRPC server exposes
 * the identity contract to other FinPay services.
 */
@Configuration
public class GrpcServerConfig {

    @Value("${finpay.identity.grpc.port:9091}")
    private int grpcPort;

    @Bean
    public Server grpcServer(GrpcIdentityService identityService) throws IOException {
        Server server = NettyServerBuilder.forPort(grpcPort)
                .addService(identityService)
                .build()
                .start();
        return server;
    }

    /** Graceful shutdown hook so the JVM does not leak the gRPC port. */
    @Bean
    public ApplicationRunner grpcServerLifecycle(Server grpcServer) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        grpcServer.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
        };
    }
}
