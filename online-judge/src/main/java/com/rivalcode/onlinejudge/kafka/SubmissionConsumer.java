package com.rivalcode.onlinejudge.kafka;

import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.onlinejudge.service.JudgeService;
import com.rivalcode.onlinejudge.utils.ThreadBoxIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionConsumer {

    private final JudgeService judgeService;
    private final KafkaTemplate<String, JudgeResult> kafkaTemplate;
    private final ThreadBoxIdProvider boxIdProvider;

    @Value("${app.kafka.result-topic:submission-results}")
    private String resultTopic;

    @KafkaListener(topics = "${app.kafka.submission-topic}", groupId = "online-judge-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ComputingTask task) {
        int boxId = boxIdProvider.getBoxId();
        String submissionId = task != null ? task.getSubmissionId() : null;
        log.info("Received submission task {} for box {}", submissionId, boxId);
        
        JudgeResult result;
        try {
            result = judgeService.judge(task, boxId);
            log.info("Judging finished for {}. Status: {}", submissionId, result.getOverallStatus());
        } catch (Exception e) {
            log.error("Fatal error during judging", e);
            result = JudgeResult.builder()
                    .submissionId(submissionId)
                    .overallStatus(JudgeStatus.SYSTEM_ERROR)
                    .compilationError(e.getMessage())
                    .build();
        }

        publishResult(result);
    }

    private void publishResult(JudgeResult result) {
        try {
            kafkaTemplate.send(resultTopic, result.getSubmissionId(), result).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing judge result " + result.getSubmissionId(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish judge result " + result.getSubmissionId(), e);
        }
    }
}
