package com.rivalcode.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayServiceApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void routeDiagnosticsEndpointReturnsConfiguredRoutes() {
        webTestClient.get()
                .uri("/api/gateway/routes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(8)
                .jsonPath("$[?(@.routeId == 'duels-ws')].pathPattern").isEqualTo("/ws/duels/**");
    }
}
