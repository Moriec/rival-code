package com.rivalcode.gatewayservice.model;

import java.net.URI;
import java.util.List;

public record GatewayRouteDefinition(
        String routeId,
        String pathPattern,
        String targetService,
        URI targetUri,
        List<String> methods,
        boolean enabled,
        RouteTransport transport
) {
}
