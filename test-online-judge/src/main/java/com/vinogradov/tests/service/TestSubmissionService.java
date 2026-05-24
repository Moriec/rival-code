package com.vinogradov.tests.service;

import com.vinogradov.contracts.submissions.model.ComputingTask;
import com.vinogradov.contracts.submissionResult.model.JudgeResult;
import com.vinogradov.tests.kafka.ResultListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service

@RequiredArgsConstructor
@Slf4j
public class TestSubmissionService {

    private final KafkaTemplate<String, ComputingTask> kafkaTemplate;
    private final ResultListener resultListener;

    @Value("${app.kafka.submission-topic:submissions}")
    private String submissionTopic;

    public JudgeResult submitAndWaitForResult(ComputingTask task) {
        String submissionId = task.getSubmissionId();

        log.info("Submitting task: {}", submissionId);
        kafkaTemplate.send(submissionTopic, submissionId, task);

        log.info("Waiting for result for submission: {}", submissionId);
        try {
            Thread.sleep(100);
            resultListener.waitForResult(submissionId);

            JudgeResult result = resultListener.getResult(submissionId);
            if (result == null) {
                log.warn("No result received for submission: {}", submissionId);
                return null;
            }

            log.info("Got result for submission: {} - Status: {}", submissionId, result.getOverallStatus());
            return result;
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for result: {}", submissionId, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
