package com.rivalcode.submissionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.model.ProblemExecutionContextDto;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.starter.events.EventEnvelopeFactory;
import com.rivalcode.submissionservice.client.ProblemServiceClient;
import com.rivalcode.submissionservice.repository.ExecutionSnapshotRepository;
import com.rivalcode.submissionservice.repository.JudgeResultRepository;
import com.rivalcode.submissionservice.repository.OutboxEventRepository;
import com.rivalcode.submissionservice.repository.ProcessedJudgeResultRepository;
import com.rivalcode.submissionservice.repository.SubmissionPublicViewRepository;
import com.rivalcode.submissionservice.repository.SubmissionRepository;
import com.rivalcode.submissionservice.repository.TestCaseResultRepository;
import com.rivalcode.submissionservice.storage.MinioTestSuiteResolver;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmissionServiceJudgeResultTest {

    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final ExecutionSnapshotRepository executionSnapshotRepository = mock(ExecutionSnapshotRepository.class);
    private final JudgeResultRepository judgeResultRepository = mock(JudgeResultRepository.class);
    private final TestCaseResultRepository testCaseResultRepository = mock(TestCaseResultRepository.class);
    private final SubmissionPublicViewRepository publicViewRepository = mock(SubmissionPublicViewRepository.class);
    private final ProcessedJudgeResultRepository processedJudgeResultRepository =
            mock(ProcessedJudgeResultRepository.class);
    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final ProblemServiceClient problemServiceClient = mock(ProblemServiceClient.class);
    private final MinioTestSuiteResolver testSuiteResolver = mock(MinioTestSuiteResolver.class);
    private final EventEnvelopeFactory eventEnvelopeFactory = mock(EventEnvelopeFactory.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, ComputingTask> computingTaskKafkaTemplate = mock(KafkaTemplate.class);

    @Test
    void handleJudgeResultSkipsNonUuidSubmissionId() {
        SubmissionService service = service();
        JudgeResult result = JudgeResult.builder()
                .submissionId("test-123")
                .build();

        assertDoesNotThrow(() -> service.handleJudgeResult(result));

        verifyNoInteractions(
                submissionRepository,
                executionSnapshotRepository,
                judgeResultRepository,
                testCaseResultRepository,
                publicViewRepository,
                processedJudgeResultRepository,
                outboxEventRepository
        );
    }

    @Test
    void handleJudgeResultSkipsUnknownSubmissionId() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        SubmissionService service = service();
        JudgeResult result = JudgeResult.builder()
                .submissionId(submissionId.toString())
                .build();

        assertDoesNotThrow(() -> service.handleJudgeResult(result));

        verify(submissionRepository).findById(submissionId);
        verifyNoInteractions(
                executionSnapshotRepository,
                judgeResultRepository,
                testCaseResultRepository,
                publicViewRepository,
                processedJudgeResultRepository,
                outboxEventRepository
        );
    }

    @Test
    void resolvesCustomCheckerCodeFromProblemExecutionContext() {
        SubmissionService service = service();
        ProblemExecutionContextDto context = ProblemExecutionContextDto.builder()
                .checkerType(CheckerType.CUSTOM)
                .customCheckerObjectKey("s3://olimp-tests/2010/C2/checker.py")
                .build();
        when(testSuiteResolver.readUtf8("s3://olimp-tests/2010/C2/checker.py")).thenReturn("print(1)");

        String checkerCode = ReflectionTestUtils.invokeMethod(service, "resolveCustomCheckerCode", context);

        assertThat(checkerCode).isEqualTo("print(1)");
    }

    private SubmissionService service() {
        return new SubmissionService(
                submissionRepository,
                executionSnapshotRepository,
                judgeResultRepository,
                testCaseResultRepository,
                publicViewRepository,
                processedJudgeResultRepository,
                outboxEventRepository,
                problemServiceClient,
                testSuiteResolver,
                computingTaskKafkaTemplate,
                eventEnvelopeFactory,
                new ObjectMapper(),
                transactionManager
        );
    }
}
