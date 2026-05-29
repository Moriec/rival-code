package com.rivalcode.submissionservice.kafka;

import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.submissionservice.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeResultConsumer {

    private final SubmissionService submissionService;

    @KafkaListener(
            topics = "${app.kafka.result-topic:submission-results}",
            groupId = "${app.kafka.result-group-id:submission-service}",
            containerFactory = "judgeResultKafkaListenerContainerFactory"
    )
    public void consume(JudgeResult result) {
        log.info("Received JudgeResult for submission {}", result != null ? result.getSubmissionId() : null);
        submissionService.handleJudgeResult(result);
    }
}
