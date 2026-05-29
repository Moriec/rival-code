package com.rivalcode.gatewayservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.gateway")
public class GatewayProperties {

    @NotBlank
    private String traceHeaderName = "X-Trace-Id";

    @Min(100)
    private long statusTimeoutMs = 1500;

    @Valid
    private Cors cors = new Cors();

    @Valid
    private RateLimit rateLimit = new RateLimit();

    @Valid
    private Services services = new Services();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedOriginPatterns = new ArrayList<>();
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private List<String> exposedHeaders = List.of("X-Trace-Id");
        private boolean allowCredentials = true;
        private long maxAgeSeconds = 3600;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;

        @Min(1)
        private int requestsPerMinute = 600;
    }

    @Getter
    @Setter
    public static class Services {
        @Valid
        private TargetService auth = new TargetService();

        @Valid
        private TargetService problem = new TargetService();

        @Valid
        private TargetService submission = new TargetService();

        @Valid
        private TargetService notification = new TargetService();

        @Valid
        private TargetService duel = new TargetService();
    }

    @Getter
    @Setter
    public static class TargetService {
        private URI httpUri;
        private URI wsUri;
    }
}
