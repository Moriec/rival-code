package com.rivalcode.gatewayservice.controller;

import com.rivalcode.contracts.gateway.model.GatewayRouteDto;
import com.rivalcode.contracts.gateway.model.GatewayServiceStatusDto;
import com.rivalcode.gatewayservice.service.GatewayRouteService;
import com.rivalcode.gatewayservice.service.GatewayStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
@Tag(name = "Gateway", description = "Gateway diagnostics and route catalog")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayRouteService routeService;
    private final GatewayStatusService statusService;

    @GetMapping("/routes")
    @Operation(summary = "Get active gateway routes")
    public List<GatewayRouteDto> routes() {
        return routeService.getRoutes();
    }

    @GetMapping("/services/status")
    @Operation(summary = "Get backend service status")
    public Mono<List<GatewayServiceStatusDto>> serviceStatuses() {
        return statusService.getServiceStatuses();
    }

    @GetMapping("/info")
    @Operation(summary = "Get gateway info")
    public Map<String, Object> info() {
        return Map.of(
                "service", "gateway-service",
                "version", "1.0-SNAPSHOT",
                "routes", routeService.getRoutes().size()
        );
    }
}
