package com.rivalcode.starter.tracing;

import org.slf4j.MDC;

import java.util.Optional;

public class MdcTraceIdProvider implements TraceIdProvider {

    @Override
    public Optional<String> currentTraceId() {
        return Optional.ofNullable(MDC.get(TraceConstants.MDC_TRACE_ID_KEY));
    }
}
