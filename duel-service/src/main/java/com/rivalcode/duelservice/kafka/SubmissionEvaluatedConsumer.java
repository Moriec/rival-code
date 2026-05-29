package com.rivalcode.duelservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.common.EventEnvelope;
import com.rivalcode.contracts.submissions.model.SubmissionEvaluatedEvent;
import com.rivalcode.duelservice.service.DuelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionEvaluatedConsumer {

    private final DuelService duelService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.submission-events-topic:submission-events.v1}",
            containerFactory = "submissionEventKafkaListenerContainerFactory"
    )
    public void consume(EventEnvelope envelope, Acknowledgment acknowledgment) {
        try {
            if (envelope == null || envelope.getPayload() == null) {
                acknowledgment.acknowledge();
                return;
            }
            SubmissionEvaluatedEvent event = objectMapper.convertValue(
                    envelope.getPayload(),
                    SubmissionEvaluatedEvent.class
            );
            duelService.handleSubmissionEvaluated(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process submission evaluated event", e);
            throw e;
        }
    }
}
