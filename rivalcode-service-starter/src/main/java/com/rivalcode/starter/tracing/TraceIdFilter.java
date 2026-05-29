package com.rivalcode.starter.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {

    private final String traceHeaderName;

    public TraceIdFilter(String traceHeaderName) {
        this.traceHeaderName = traceHeaderName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        MDC.put(TraceConstants.MDC_TRACE_ID_KEY, traceId);
        response.setHeader(traceHeaderName, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incomingTraceId = request.getHeader(traceHeaderName);
        if (StringUtils.hasText(incomingTraceId)) {
            return incomingTraceId.trim();
        }
        return UUID.randomUUID().toString();
    }
}
