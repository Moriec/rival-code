package com.rivalcode.gatewayservice.service;

import com.rivalcode.contracts.gateway.model.GatewayRouteDto;
import com.rivalcode.gatewayservice.config.GatewayRouteCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GatewayRouteService {

    private final GatewayRouteCatalog routeCatalog;

    public List<GatewayRouteDto> getRoutes() {
        return routeCatalog.routes().stream()
                .map(route -> GatewayRouteDto.builder()
                        .routeId(route.routeId())
                        .pathPattern(route.pathPattern())
                        .targetService(route.targetService())
                        .targetUri(route.targetUri() != null ? route.targetUri().toString() : null)
                        .methods(route.methods())
                        .enabled(route.enabled())
                        .build())
                .toList();
    }
}
