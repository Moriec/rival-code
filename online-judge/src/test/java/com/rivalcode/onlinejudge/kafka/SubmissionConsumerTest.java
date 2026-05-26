package com.rivalcode.onlinejudge.kafka;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.service.JudgeService;
import com.rivalcode.onlinejudge.utils.ThreadBoxIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionConsumerTest {

    @Mock private JudgeService judgeService;
    @Mock private KafkaTemplate<String, JudgeResult> kafkaTemplate;
    @Mock private ThreadBoxIdProvider boxIdProvider;
    private SubmissionConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new SubmissionConsumer(judgeService, kafkaTemplate, boxIdProvider);
        ReflectionTestUtils.setField(consumer, "resultTopic", "submission-results");
        when(boxIdProvider.getBoxId()).thenReturn(0);
    }

    @Test
    void shouldWaitForJudgeResultPublication() {
        ComputingTask task = task("sub-1");
        JudgeResult result = JudgeResult.builder()
                .submissionId("sub-1")
                .overallStatus(JudgeStatus.ACCEPTED)
                .build();
        when(judgeService.judge(task, 0)).thenReturn(result);
        when(kafkaTemplate.send("submission-results", "sub-1", result)).thenReturn(CompletableFuture.completedFuture(null));

        consumer.consume(task);

        verify(kafkaTemplate).send("submission-results", "sub-1", result);
    }

    @Test
    void shouldPublishSystemErrorWhenJudgeThrows() {
        ComputingTask task = task("sub-fatal");
        when(judgeService.judge(task, 0)).thenThrow(new RuntimeException("boom"));
        when(kafkaTemplate.send(eq("submission-results"), eq("sub-fatal"), any())).thenReturn(CompletableFuture.completedFuture(null));

        consumer.consume(task);

        ArgumentCaptor<JudgeResult> resultCaptor = ArgumentCaptor.forClass(JudgeResult.class);
        verify(kafkaTemplate).send(eq("submission-results"), eq("sub-fatal"), resultCaptor.capture());
        assertEquals(JudgeStatus.SYSTEM_ERROR, resultCaptor.getValue().getOverallStatus());
        assertEquals("boom", resultCaptor.getValue().getCompilationError());
    }

    @Test
    void shouldRethrowWhenResultPublicationFails() {
        ComputingTask task = task("sub-send-failed");
        JudgeResult result = JudgeResult.builder()
                .submissionId("sub-send-failed")
                .overallStatus(JudgeStatus.ACCEPTED)
                .build();
        when(judgeService.judge(task, 0)).thenReturn(result);
        when(kafkaTemplate.send("submission-results", "sub-send-failed", result))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        assertThrows(IllegalStateException.class, () -> consumer.consume(task));
    }

    private ComputingTask task(String submissionId) {
        return ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(new UserCode("public class Main {}", ProgrammingLanguages.JAVA))
                .build();
    }
}
