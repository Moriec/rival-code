package com.vinogradov.contracts.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> {
    private UUID eventId;
    private String eventType;
    private Integer eventVersion;
    private Instant occurredAt;
    private String traceId;
    private String producer;
    private T payload;
}
