package com.rivalcode.gatewayservice.filter;

import com.rivalcode.gatewayservice.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryRateLimitFilter implements GlobalFilter, Ordered {

    private final GatewayProperties.RateLimit properties;
    private final Map<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimitFilter(GatewayProperties properties) {
        this.properties = properties.getRateLimit();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String key = clientKey(exchange);
        long currentMinute = Instant.now().getEpochSecond() / 60;
        ClientBucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new ClientBucket(currentMinute);
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (bucket.counter.get() > properties.getRequestsPerMinute()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private String clientKey(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private static class ClientBucket {
        private final long minute;
        private final AtomicInteger counter = new AtomicInteger(1);

        private ClientBucket(long minute) {
            this.minute = minute;
        }
    }
}
