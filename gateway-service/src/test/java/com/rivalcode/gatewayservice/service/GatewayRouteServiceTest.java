package com.rivalcode.gatewayservice.service;

import com.rivalcode.gatewayservice.config.GatewayProperties;
import com.rivalcode.gatewayservice.config.GatewayRouteCatalog;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteServiceTest {

    @Test
    void returnsRouteContractsFromCatalog() {
        GatewayProperties properties = new GatewayProperties();
        properties.getServices().getAuth().setHttpUri(URI.create("http://auth-service:8081"));
        properties.getServices().getProblem().setHttpUri(URI.create("http://problem-service:8082"));
        properties.getServices().getSubmission().setHttpUri(URI.create("http://submission-service:8083"));
        properties.getServices().getNotification().setHttpUri(URI.create("http://notification-service:8084"));
        properties.getServices().getDuel().setHttpUri(URI.create("http://duel-service:8085"));
        properties.getServices().getDuel().setWsUri(URI.create("ws://duel-service:8085"));

        GatewayRouteService service = new GatewayRouteService(new GatewayRouteCatalog(properties));

        var routes = service.getRoutes();

        assertThat(routes).hasSize(8);
        assertThat(routes)
                .extracting("pathPattern")
                .contains(
                        "/api/auth/**",
                        "/api/users/*/submissions",
                        "/api/users/**",
                        "/api/problems/**",
                        "/api/duels/**",
                        "/ws/duels/**",
                        "/api/submissions/**",
                        "/api/notifications/**"
                );
    }
}
