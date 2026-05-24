package com.vinogradov.tests.kafka;

import com.vinogradov.contracts.submissionResult.model.JudgeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ResultListener {

    private final ConcurrentHashMap<String, CompletableFuture<JudgeResult>> futures = new ConcurrentHashMap<>();

    @KafkaListener(topics = "submission-results", groupId = "test-online-judge-group", containerFactory = "judgeResultKafkaListenerContainerFactory")
    public void handleResult(JudgeResult result) {
        log.info("Received result for submission: {} with status: {}", result.getSubmissionId(), result.getOverallStatus());

        futures.computeIfAbsent(result.getSubmissionId(), k -> new CompletableFuture<>())
                .complete(result);
    }

    public JudgeResult getResult(String submissionId) {
        CompletableFuture<JudgeResult> future = futures.get(submissionId);
        return (future != null && future.isDone()) ? future.join() : null;
    }

    public void waitForResult(String submissionId) {
        try {
            futures.computeIfAbsent(submissionId, k -> new CompletableFuture<>())
                    .join();
        } catch (Exception e) {
            log.error("Interrupted while waiting for result {}", submissionId, e);
            Thread.currentThread().interrupt();
        }
    }

    public void clearResults() {
        futures.clear();
    }
}