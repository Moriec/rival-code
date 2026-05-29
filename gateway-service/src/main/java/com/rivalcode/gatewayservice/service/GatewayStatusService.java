package com.rivalcode.gatewayservice.service;

import com.rivalcode.contracts.gateway.model.GatewayServiceStatusDto;
import com.rivalcode.gatewayservice.config.GatewayProperties;
import com.rivalcode.gatewayservice.config.GatewayRouteCatalog;
import com.rivalcode.gatewayservice.model.GatewayRouteDefinition;
import com.rivalcode.gatewayservice.model.RouteTransport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GatewayStatusService {

    private final GatewayRouteCatalog routeCatalog;
    private final GatewayProperties properties;
    private final WebClient.Builder webClientBuilder;

    public Mono<List<GatewayServiceStatusDto>> getServiceStatuses() {
        Map<String, URI> targetsByService = new LinkedHashMap<>();
        routeCatalog.routes().stream()
                .filter(route -> route.transport() == RouteTransport.HTTP)
                .filter(GatewayRouteDefinition::enabled)
                .filter(route -> route.targetUri() != null)
                .forEach(route -> targetsByService.putIfAbsent(route.targetService(), route.targetUri()));

        return Flux.fromIterable(targetsByService.entrySet())
                .flatMap(entry -> checkService(entry.getKey(), entry.getValue()))
                .collectList();
    }

    private Mono<GatewayServiceStatusDto> checkService(String serviceName, URI targetUri) {
        Instant startedAt = Instant.now();
        return webClientBuilder.build()
                .get()
                .uri(targetUri)
                .exchangeToMono(response -> {
                    long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
                    int statusCode = response.statusCode().value();
                    String status = statusCode >= 500 ? "DEGRADED" : "UP";
                    return Mono.just(GatewayServiceStatusDto.builder()
                            .serviceName(serviceName)
                            .status(status)
                            .version("unknown")
                            .checkedAt(Instant.now())
                            .details(Map.of(
                                    "targetUri", targetUri.toString(),
                                    "httpStatus", statusCode,
                                    "latencyMs", latencyMs
                            ))
                            .build());
                })
                .timeout(Duration.ofMillis(properties.getStatusTimeoutMs()))
                .onErrorResume(error -> Mono.just(GatewayServiceStatusDto.builder()
                        .serviceName(serviceName)
                        .status("DOWN")
                        .version("unknown")
                        .checkedAt(Instant.now())
                        .details(Map.of(
                                "targetUri", targetUri.toString(),
                                "error", error.getClass().getSimpleName(),
                                "message", error.getMessage() != null ? error.getMessage() : "unavailable"
                        ))
                        .build()));
    }
}
