package com.rivalcode.gatewayservice.config;

import com.rivalcode.gatewayservice.model.GatewayRouteDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder, GatewayRouteCatalog routeCatalog) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        for (GatewayRouteDefinition route : routeCatalog.routes()) {
            if (!route.enabled() || route.targetUri() == null) {
                continue;
            }

            routes.route(route.routeId(), spec -> spec
                    .path(route.pathPattern())
                    .uri(route.targetUri().toString()));
        }

        return routes.build();
    }
}
