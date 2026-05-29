package com.rivalcode.gatewayservice.config;

import com.rivalcode.gatewayservice.model.GatewayRouteDefinition;
import com.rivalcode.gatewayservice.model.RouteTransport;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatewayRouteCatalog {

    private final GatewayProperties properties;

    public GatewayRouteCatalog(GatewayProperties properties) {
        this.properties = properties;
    }

    public List<GatewayRouteDefinition> routes() {
        return List.of(
                new GatewayRouteDefinition(
                        "auth-api",
                        "/api/auth/**",
                        "auth-service",
                        properties.getServices().getAuth().getHttpUri(),
                        List.of("ALL"),
                        true,
                        RouteTransport.HTTP
                ),
                new GatewayRouteDefinition(
                        "user-submissions-api",
                        "/api/users/*/submissions",
                        "submission-service",
                        properties.getServices().getSubmission().getHttpUri(),
                        List.of("GET"),
                        true,
                        RouteTransport.HTTP
                ),
                new GatewayRouteDefinition(
                        "users-api",
                        "/api/users/**",
                        "auth-service",
                        properties.getServices().getAuth().getHttpUri(),
                        List.of("ALL"),
                        true,
                        RouteTransport.HTTP
                ),
                new GatewayRouteDefinition(
                        "problems-api",
                        "/api/problems/**",
                        "problem-service",
                        properties.getServices().getProblem().getHttpUri(),
                        List.of("ALL"),
                        true,
                        RouteTransport.HTTP
                ),
                new GatewayRouteDefinition(
                        "duels-api",
                        "/api/duels/**",
                        "duel-service",
                        properties.getServices().getDuel().getHttpUri(),
                        List.of("ALL"),
                        true,
                        RouteTransport.HTTP
                ),
                new GatewayRouteDefinition(
                        "duels-ws",
                        "/ws/duels/**",
                        "duel-service",
                        properties.getServices().getDuel().getWsUri(),
                        List.of("GET"),
                        true,
                        RouteTransport.WEBSOCKET
                ),
                new GatewayRouteDefinition(
                        "submissions-api",
                        "/api/submissions/**",
                        "submission-service",
                        properties.getServices().getSubmission().getHttpUri(),
                        List.of("ALL"),
                        true,
                        RouteTransport.HTTP
                ),
                new GatewayRouteDefinition(
                        "notifications-api",
                        "/api/notifications/**",
                        "notification-service",
                        properties.getServices().getNotification().getHttpUri(),
                        List.of("ALL"),
                        true,
                        RouteTransport.HTTP
                )
        );
    }
}
