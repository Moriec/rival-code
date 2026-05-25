package com.rivalcode.starter.events;

import com.rivalcode.contracts.common.EventEnvelope;
import com.rivalcode.starter.service.ServiceInfo;
import com.rivalcode.starter.tracing.TraceIdProvider;

import java.time.Instant;
import java.util.UUID;

public class EventEnvelopeFactory {

    private final ServiceInfo serviceInfo;
    private final TraceIdProvider traceIdProvider;

    public EventEnvelopeFactory(ServiceInfo serviceInfo, TraceIdProvider traceIdProvider) {
        this.serviceInfo = serviceInfo;
        this.traceIdProvider = traceIdProvider;
    }

    public <T> EventEnvelope<T> create(String eventType, int eventVersion, T payload) {
        String traceId = traceIdProvider.currentTraceId().orElse(null);
        return create(eventType, eventVersion, traceId, payload);
    }

    public <T> EventEnvelope<T> create(String eventType, int eventVersion, String traceId, T payload) {
        EventEnvelope<T> envelope = new EventEnvelope<>();
        envelope.setEventId(UUID.randomUUID());
        envelope.setEventType(eventType);
        envelope.setEventVersion(eventVersion);
        envelope.setOccurredAt(Instant.now());
        envelope.setTraceId(traceId);
        envelope.setProducer(serviceInfo.name());
        envelope.setPayload(payload);
        return envelope;
    }
}
