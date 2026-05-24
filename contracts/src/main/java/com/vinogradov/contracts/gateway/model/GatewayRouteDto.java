package com.vinogradov.contracts.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRouteDto {
    private String routeId;
    private String pathPattern;
    private String targetService;
    private String targetUri;
    private List<String> methods;
    private Boolean enabled;
}
