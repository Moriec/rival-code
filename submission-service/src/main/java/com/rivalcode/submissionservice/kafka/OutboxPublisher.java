package com.rivalcode.submissionservice.kafka;

import com.rivalcode.submissionservice.model.OutboxEventEntity;
import com.rivalcode.submissionservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final String PENDING = "PENDING";
    private static final String SENT = "SENT";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    @Value("${app.kafka.send-timeout-ms:10000}")
    private long kafkaSendTimeoutMs;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEventEntity event : outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(PENDING)) {
            try {
                stringKafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayloadJson().toString())
                        .get(kafkaSendTimeoutMs, TimeUnit.MILLISECONDS);
                event.setStatus(SENT);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {}", event.getOutboxEventId(), e);
                event.setLastError(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }
}
