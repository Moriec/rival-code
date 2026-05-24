package com.vinogradov.onlinejudge.kafka;

import com.vinogradov.contracts.submissions.model.ComputingTask;
import com.vinogradov.contracts.submissionResult.model.JudgeResult;
import com.vinogradov.onlinejudge.service.JudgeService;
import com.vinogradov.onlinejudge.utils.ThreadBoxIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @KafkaListener(topics = "${app.kafka.submission-topic}", groupId = "online-judge-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ComputingTask task) {
        int boxId = boxIdProvider.getBoxId();
        log.info("Received submission task {} for box {}", task.getSubmissionId(), boxId);
        
        try {
            JudgeResult result = judgeService.judge(task, boxId);
            
            log.info("Judging finished for {}. Status: {}", task.getSubmissionId(), result.getOverallStatus());
            
            kafkaTemplate.send("submission-results", result.getSubmissionId(), result);
            
        } catch (Exception e) {
            log.error("Fatal error during judging", e);
        }
    }
}
