package com.rivalcode.submissionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.common.EventEnvelope;
import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.model.ProblemExecutionContextDto;
import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissionResult.model.TestCaseResult;
import com.rivalcode.contracts.submissions.enums.SubmissionMode;
import com.rivalcode.contracts.submissions.enums.SubmissionStatus;
import com.rivalcode.contracts.submissions.model.*;
import com.rivalcode.starter.events.EventEnvelopeFactory;
import com.rivalcode.submissionservice.client.ProblemServiceClient;
import com.rivalcode.submissionservice.model.*;
import com.rivalcode.submissionservice.repository.*;
import com.rivalcode.submissionservice.storage.MinioTestSuiteResolver;
import com.rivalcode.submissionservice.storage.ResolvedTestSuite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Service
public class SubmissionService {

    private static final String SUBMISSION_EVALUATED = "SUBMISSION_EVALUATED";
    private static final String OUTBOX_PENDING = "PENDING";

    private final SubmissionRepository submissionRepository;
    private final ExecutionSnapshotRepository executionSnapshotRepository;
    private final JudgeResultRepository judgeResultRepository;
    private final TestCaseResultRepository testCaseResultRepository;
    private final SubmissionPublicViewRepository publicViewRepository;
    private final ProcessedJudgeResultRepository processedJudgeResultRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProblemServiceClient problemServiceClient;
    private final MinioTestSuiteResolver testSuiteResolver;
    private final KafkaTemplate<String, ComputingTask> computingTaskKafkaTemplate;
    private final EventEnvelopeFactory eventEnvelopeFactory;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.kafka.submission-topic:submissions}")
    private String submissionTopic;

    @Value("${app.kafka.submission-events-topic:submission-events.v1}")
    private String submissionEventsTopic;

    @Value("${app.kafka.send-timeout-ms:10000}")
    private long kafkaSendTimeoutMs;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            ExecutionSnapshotRepository executionSnapshotRepository,
            JudgeResultRepository judgeResultRepository,
            TestCaseResultRepository testCaseResultRepository,
            SubmissionPublicViewRepository publicViewRepository,
            ProcessedJudgeResultRepository processedJudgeResultRepository,
            OutboxEventRepository outboxEventRepository,
            ProblemServiceClient problemServiceClient,
            MinioTestSuiteResolver testSuiteResolver,
            KafkaTemplate<String, ComputingTask> computingTaskKafkaTemplate,
            EventEnvelopeFactory eventEnvelopeFactory,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.submissionRepository = submissionRepository;
        this.executionSnapshotRepository = executionSnapshotRepository;
        this.judgeResultRepository = judgeResultRepository;
        this.testCaseResultRepository = testCaseResultRepository;
        this.publicViewRepository = publicViewRepository;
        this.processedJudgeResultRepository = processedJudgeResultRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.problemServiceClient = problemServiceClient;
        this.testSuiteResolver = testSuiteResolver;
        this.computingTaskKafkaTemplate = computingTaskKafkaTemplate;
        this.eventEnvelopeFactory = eventEnvelopeFactory;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public SubmissionCreatedResponse createSubmission(CreateSubmissionRequest request, UUID currentUserId) {
        ValidatedCreateRequest validRequest = validateCreateRequest(request, currentUserId);
        UUID submissionId = UUID.randomUUID();
        UUID requestedProblemVersionId = parseOptionalUuid(request.getProblemVersionId(), "problemVersionId");

        ProblemExecutionContextDto problemContext =
                problemServiceClient.getExecutionContext(validRequest.problemId(), requestedProblemVersionId);
        UUID snapshotProblemVersionId = parseRequiredUuid(problemContext.getProblemVersionId(), "problemVersionId");
        ResolvedTestSuite testSuite = testSuiteResolver.resolve(problemContext.getTestArchiveObjectKey());
        String customCheckerCode = resolveCustomCheckerCode(problemContext);

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId.toString())
                .userCode(request.getUserCode())
                .testCases(testSuite.testCases())
                .timeLimitMs(problemContext.getLimits().getTimeLimitMs())
                .memoryLimitKb(problemContext.getLimits().getMemoryLimitKb())
                .outputLimitBytes(problemContext.getLimits().getOutputLimitBytes())
                .customCheckerCode(customCheckerCode)
                .build();

        Instant createdAt = Instant.now();
        SubmissionEntity submission = SubmissionEntity.builder()
                .submissionId(submissionId)
                .userId(validRequest.userId())
                .problemId(validRequest.problemId())
                .problemVersionId(snapshotProblemVersionId)
                .duelId(validRequest.duelId())
                .mode(request.getMode())
                .language(request.getUserCode().getLanguage())
                .sourceCode(request.getUserCode().getSourceCode())
                .status(SubmissionStatus.CREATED)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        ExecutionSnapshotEntity snapshot = ExecutionSnapshotEntity.builder()
                .submissionId(submissionId)
                .problemVersionId(snapshotProblemVersionId)
                .timeLimitMs(problemContext.getLimits().getTimeLimitMs())
                .memoryLimitKb(problemContext.getLimits().getMemoryLimitKb())
                .outputLimitBytes(problemContext.getLimits().getOutputLimitBytes())
                .checkerType(problemContext.getCheckerType())
                .customCheckerCode(customCheckerCode)
                .testArchiveObjectKey(problemContext.getTestArchiveObjectKey())
                .visibleSampleTestsCount(nullToZero(problemContext.getVisibleSampleTestsCount()))
                .storeFullJudgeLog(true)
                .ratedMode(request.getMode() == SubmissionMode.DUEL)
                .snapshotJson(toJsonNode(task))
                .createdAt(createdAt)
                .build();

        transactionTemplate.executeWithoutResult(status -> {
            submissionRepository.save(submission);
            executionSnapshotRepository.save(snapshot);
        });

        try {
            computingTaskKafkaTemplate.send(submissionTopic, submissionId.toString(), task)
                    .get(kafkaSendTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            markSubmissionFailed(submissionId);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to queue submission for judging", e);
        }

        Instant queuedAt = Instant.now();
        transactionTemplate.executeWithoutResult(status -> {
            SubmissionEntity queuedSubmission = getSubmissionEntity(submissionId);
            queuedSubmission.setStatus(SubmissionStatus.QUEUED);
            queuedSubmission.setQueuedAt(queuedAt);
            queuedSubmission.setUpdatedAt(queuedAt);
            submissionRepository.save(queuedSubmission);
        });

        return SubmissionCreatedResponse.builder()
                .submissionId(submissionId.toString())
                .status(SubmissionStatus.QUEUED)
                .createdAt(createdAt)
                .build();
    }

    @Transactional(readOnly = true)
    public SubmissionSummaryDto getSubmission(UUID submissionId) {
        return toSummaryDto(getSubmissionEntity(submissionId));
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummaryDto> getUserSubmissions(UUID userId) {
        return submissionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicSubmissionVerdict getVerdict(UUID submissionId) {
        SubmissionPublicViewEntity view = publicViewRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission verdict is not ready"));

        return PublicSubmissionVerdict.builder()
                .submissionId(submissionId.toString())
                .overallStatus(view.getOverallStatus())
                .maxTimeMs(view.getMaxTimeMs())
                .maxMemoryKb(view.getMaxMemoryKb())
                .passedTests(view.getPassedTests())
                .totalTests(view.getTotalTests())
                .compilationError(view.getCompilationError())
                .visibleTests(fromJson(view.getVisibleTestsJson(), new TypeReference<>() {
                }))
                .build();
    }

    @Transactional(readOnly = true)
    public SubmissionExecutionContext getExecutionContext(UUID submissionId) {
        ExecutionSnapshotEntity snapshot = executionSnapshotRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution context not found"));

        return SubmissionExecutionContext.builder()
                .submissionId(submissionId.toString())
                .problemVersionId(snapshot.getProblemVersionId().toString())
                .timeLimitMs(snapshot.getTimeLimitMs())
                .memoryLimitKb(snapshot.getMemoryLimitKb())
                .checkerType(snapshot.getCheckerType())
                .testArchiveObjectKey(snapshot.getTestArchiveObjectKey())
                .visibleSampleTestsCount(snapshot.getVisibleSampleTestsCount())
                .storeFullJudgeLog(snapshot.getStoreFullJudgeLog())
                .ratedMode(snapshot.getRatedMode())
                .build();
    }

    @Transactional
    public void handleJudgeResult(JudgeResult result) {
        if (result == null) {
            log.warn("Skipping empty JudgeResult message");
            return;
        }

        UUID submissionId = tryParseJudgeSubmissionId(result.getSubmissionId());
        if (submissionId == null) {
            return;
        }

        SubmissionEntity submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.debug("Skipping JudgeResult for unknown submission {}", submissionId);
            return;
        }

        String rawResultJson = toJson(result);
        String eventHash = sha256(rawResultJson);

        processedJudgeResultRepository.findById(submissionId).ifPresent(processed -> {
            if (!Objects.equals(processed.getResultEventHash(), eventHash)) {
                throw new IllegalStateException("Conflicting JudgeResult for submission " + submissionId);
            }
        });
        if (processedJudgeResultRepository.existsById(submissionId)) {
            return;
        }

        ExecutionSnapshotEntity snapshot = executionSnapshotRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Execution snapshot not found for " + submissionId));
        List<TestCaseResult> testCaseResults = result.getTestCaseResults() != null
                ? result.getTestCaseResults()
                : List.of();

        JudgeStatus overallStatus = result.getOverallStatus() != null
                ? result.getOverallStatus()
                : JudgeStatus.SYSTEM_ERROR;
        int passedTests = (int) testCaseResults.stream()
                .filter(test -> test.getStatus() == JudgeStatus.ACCEPTED)
                .count();
        int totalTests = testCaseResults.size();
        boolean accepted = overallStatus == JudgeStatus.ACCEPTED;
        Instant now = Instant.now();

        judgeResultRepository.save(JudgeResultEntity.builder()
                .submissionId(submissionId)
                .overallStatus(overallStatus)
                .maxTimeMs(result.getMaxTimeMs())
                .maxMemoryKb(result.getMaxMemoryKb())
                .compilationError(result.getCompilationError())
                .rawResultJson(toJsonNode(result))
                .receivedAt(now)
                .build());

        List<TestCaseResultEntity> persistedResults = IntStream.range(0, testCaseResults.size())
                .mapToObj(index -> toTestCaseResultEntity(submissionId, index + 1, testCaseResults.get(index),
                        index < snapshot.getVisibleSampleTestsCount()))
                .toList();
        testCaseResultRepository.saveAll(persistedResults);

        List<PublicTestCaseResult> visibleTests = persistedResults.stream()
                .filter(TestCaseResultEntity::getVisible)
                .map(this::toPublicTestCaseResult)
                .toList();

        publicViewRepository.save(SubmissionPublicViewEntity.builder()
                .submissionId(submissionId)
                .overallStatus(overallStatus)
                .passedTests(passedTests)
                .totalTests(totalTests)
                .maxTimeMs(result.getMaxTimeMs())
                .maxMemoryKb(result.getMaxMemoryKb())
                .compilationError(result.getCompilationError())
                .visibleTestsJson(toJsonNode(visibleTests))
                .updatedAt(now)
                .build());

        submission.setStatus(SubmissionStatus.JUDGED);
        submission.setOverallStatus(overallStatus);
        submission.setAccepted(accepted);
        submission.setMaxTimeMs(result.getMaxTimeMs());
        submission.setMaxMemoryKb(result.getMaxMemoryKb());
        submission.setJudgedAt(now);
        submission.setUpdatedAt(now);
        submissionRepository.save(submission);

        processedJudgeResultRepository.save(ProcessedJudgeResultEntity.builder()
                .submissionId(submissionId)
                .resultEventHash(eventHash)
                .processedAt(now)
                .build());

        SubmissionEvaluatedEvent event = SubmissionEvaluatedEvent.builder()
                .submissionId(submissionId.toString())
                .userId(submission.getUserId().toString())
                .duelId(submission.getDuelId() != null ? submission.getDuelId().toString() : null)
                .problemId(submission.getProblemId().toString())
                .problemVersionId(submission.getProblemVersionId().toString())
                .mode(submission.getMode())
                .overallStatus(overallStatus)
                .accepted(accepted)
                .maxTimeMs(result.getMaxTimeMs())
                .maxMemoryKb(result.getMaxMemoryKb())
                .passedTests(passedTests)
                .totalTests(totalTests)
                .build();

        EventEnvelope<SubmissionEvaluatedEvent> envelope =
                eventEnvelopeFactory.create(SUBMISSION_EVALUATED, 1, event);
        outboxEventRepository.save(OutboxEventEntity.builder()
                .outboxEventId(UUID.randomUUID())
                .eventType(SUBMISSION_EVALUATED)
                .aggregateId(submissionId)
                .topic(submissionEventsTopic)
                .messageKey(StringUtils.hasText(event.getDuelId()) ? event.getDuelId() : event.getSubmissionId())
                .payloadJson(toJsonNode(envelope))
                .status(OUTBOX_PENDING)
                .createdAt(now)
                .build());
    }

    private ValidatedCreateRequest validateCreateRequest(CreateSubmissionRequest request, UUID currentUserId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is required");
        }
        UUID userId = StringUtils.hasText(request.getUserId())
                ? parseRequiredUuid(request.getUserId(), "userId")
                : currentUserId;
        if (!currentUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create submission for another user");
        }
        UUID problemId = parseRequiredUuid(request.getProblemId(), "problemId");
        UUID duelId = parseOptionalUuid(request.getDuelId(), "duelId");
        if (request.getMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode is required");
        }
        if (request.getUserCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userCode is required");
        }
        if (request.getUserCode().getLanguage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userCode.language is required");
        }
        if (!StringUtils.hasText(request.getUserCode().getSourceCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userCode.sourceCode is required");
        }
        return new ValidatedCreateRequest(userId, problemId, duelId);
    }

    private String resolveCustomCheckerCode(ProblemExecutionContextDto problemContext) {
        if (problemContext.getCheckerType() != CheckerType.CUSTOM
                || !StringUtils.hasText(problemContext.getCustomCheckerObjectKey())) {
            return null;
        }
        return testSuiteResolver.readUtf8(problemContext.getCustomCheckerObjectKey());
    }

    private void markSubmissionFailed(UUID submissionId) {
        transactionTemplate.executeWithoutResult(status -> submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setUpdatedAt(Instant.now());
            submissionRepository.save(submission);
        }));
    }

    private SubmissionEntity getSubmissionEntity(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    private SubmissionSummaryDto toSummaryDto(SubmissionEntity submission) {
        return SubmissionSummaryDto.builder()
                .submissionId(submission.getSubmissionId().toString())
                .userId(submission.getUserId().toString())
                .problemId(submission.getProblemId().toString())
                .problemVersionId(submission.getProblemVersionId().toString())
                .duelId(submission.getDuelId() != null ? submission.getDuelId().toString() : null)
                .mode(submission.getMode())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .overallStatus(submission.getOverallStatus())
                .accepted(submission.getAccepted())
                .maxTimeMs(submission.getMaxTimeMs())
                .maxMemoryKb(submission.getMaxMemoryKb())
                .createdAt(submission.getCreatedAt())
                .judgedAt(submission.getJudgedAt())
                .build();
    }

    private TestCaseResultEntity toTestCaseResultEntity(
            UUID submissionId,
            int orderNo,
            TestCaseResult result,
            boolean visible
    ) {
        return TestCaseResultEntity.builder()
                .testCaseResultId(UUID.randomUUID())
                .submissionId(submissionId)
                .orderNo(orderNo)
                .status(result.getStatus() != null ? result.getStatus() : JudgeStatus.SYSTEM_ERROR)
                .timeMs(result.getTimeMs())
                .memoryKb(result.getMemoryKb())
                .input(result.getInput())
                .actualOutput(result.getActualOutput())
                .expectedOutput(result.getExpectedOutput())
                .message(result.getMessage())
                .visible(visible)
                .build();
    }

    private PublicTestCaseResult toPublicTestCaseResult(TestCaseResultEntity result) {
        return PublicTestCaseResult.builder()
                .orderNo(result.getOrderNo())
                .status(result.getStatus())
                .timeMs(result.getTimeMs())
                .memoryKb(result.getMemoryKb())
                .message(result.getMessage())
                .build();
    }

    private UUID parseRequiredUuid(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a UUID", e);
        }
    }

    private UUID parseOptionalUuid(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseRequiredUuid(value, fieldName);
    }

    private UUID tryParseJudgeSubmissionId(String value) {
        if (!StringUtils.hasText(value)) {
            log.warn("Skipping JudgeResult without submissionId");
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.debug("Skipping JudgeResult with non-UUID submissionId: {}", value);
            return null;
        }
    }

    private int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    private <T> T fromJson(JsonNode json, TypeReference<T> typeReference) {
        if (json == null || json.isNull()) {
            return null;
        }
        return objectMapper.convertValue(json, typeReference);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private record ValidatedCreateRequest(UUID userId, UUID problemId, UUID duelId) {
    }
}
