package com.rivalcode.gatewayservice.filter;

import com.rivalcode.gatewayservice.config.GatewayProperties;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

    private final String traceHeaderName;

    public TraceIdGatewayFilter(GatewayProperties properties) {
        this.traceHeaderName = properties.getTraceHeaderName();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange);
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(traceHeaderName, traceId))
                .build();

        exchange.getResponse().getHeaders().set(traceHeaderName, traceId);
        MDC.put("traceId", traceId);
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signalType -> MDC.remove("traceId"));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String incoming = exchange.getRequest().getHeaders().getFirst(traceHeaderName);
        if (StringUtils.hasText(incoming)) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }
}
