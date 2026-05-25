package com.rivalcode.starter.tracing;

import java.util.Optional;

public interface TraceIdProvider {

    Optional<String> currentTraceId();
}
