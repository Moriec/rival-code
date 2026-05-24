package com.vinogradov.contracts.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayServiceStatusDto {
    private String serviceName;
    private String status;
    private String version;
    private Instant checkedAt;
    private Map<String, Object> details;
}
