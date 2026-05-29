package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissionResult.model.TestCaseResult;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.onlinejudge.exception.CompilationFailedException;
import com.rivalcode.onlinejudge.model.CheckResult;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;
import com.rivalcode.onlinejudge.model.ResolvedTestCase;
import com.rivalcode.onlinejudge.service.Checker;
import com.rivalcode.onlinejudge.service.JudgeService;
import com.rivalcode.onlinejudge.service.Sandbox;
import com.rivalcode.onlinejudge.service.TestCaseContentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJudgeService implements JudgeService {

    private final Sandbox sandbox;
    private final Checker standardChecker;
    private final TestCaseContentProvider testCaseContentProvider;

    @Value("${app.sandbox.default-time-limit-ms:2000}")
    private Long defaultTimeLimitMs = 2000L;

    @Value("${app.sandbox.default-memory-limit-kb:65536}")
    private Long defaultMemoryLimitKb = 65536L;

    @Value("${app.sandbox.default-output-limit-bytes:1048576}")
    private Long defaultOutputLimitBytes = 1048576L;

    @Value("${app.judge.result-output-preview-bytes:8192}")
    private int resultOutputPreviewBytes = 8192;

    @Value("${app.judge.result-message-preview-bytes:4096}")
    private int resultMessagePreviewBytes = 4096;

    @Override
    public JudgeResult judge(ComputingTask task, int boxId) {
        String submissionId = task != null ? task.getSubmissionId() : null;
        log.info("Judging submission {} using box {}", submissionId, boxId);

        String validationError = validateTask(task);
        if (validationError != null) {
            return systemErrorResult(submissionId, validationError);
        }

        List<TestCase> testCases = task.getTestCases();
        Long timeLimitMs = task.getTimeLimitMs() != null ? task.getTimeLimitMs() : defaultTimeLimitMs;
        Long memoryLimitKb = task.getMemoryLimitKb() != null ? task.getMemoryLimitKb() : defaultMemoryLimitKb;
        Long outputLimitBytes = task.getOutputLimitBytes() != null ? task.getOutputLimitBytes() : defaultOutputLimitBytes;

        try {
            CompilationResult compilation = sandbox.compile(boxId, task.getUserCode());
            try {
                return runTestCases(task, compilation, boxId, timeLimitMs, memoryLimitKb, outputLimitBytes);
            } catch (Exception e) {
                log.error("System error while judging submission {}", submissionId, e);
                return systemErrorResult(submissionId, e.getMessage());
            } finally {
                compilation.close();
            }
        } catch (CompilationFailedException e) {
            return JudgeResult.builder()
                    .submissionId(submissionId)
                    .overallStatus(JudgeStatus.COMPILATION_ERROR)
                    .compilationError(truncateUtf8(e.getMessage(), resultMessagePreviewBytes))
                    .build();
        } catch (Exception e) {
            log.error("System error while compiling submission {}", submissionId, e);
            return systemErrorResult(submissionId, e.getMessage());
        } finally {
            sandbox.cleanup(boxId);
        }
    }

    private JudgeResult runTestCases(
            ComputingTask task,
            CompilationResult compilation,
            int boxId,
            Long timeLimitMs,
            Long memoryLimitKb,
            Long outputLimitBytes
    ) {
        List<TestCaseResult> results = new ArrayList<>();
        JudgeStatus overallStatus = JudgeStatus.ACCEPTED;
        long maxTime = 0;
        long maxMemory = 0;
        boolean hasCustomChecker = task.getCustomCheckerCode() != null && !task.getCustomCheckerCode().isBlank();

        for (TestCase testCase : task.getTestCases()) {
            ResolvedTestCase resolvedTestCase = testCaseContentProvider.resolve(testCase);
            ExecutionResult execResult = sandbox.run(
                    boxId,
                    compilation,
                    resolvedTestCase.input(),
                    timeLimitMs,
                    memoryLimitKb,
                    outputLimitBytes
            );

            JudgeStatus testStatus = execResult.getStatus();
            String message = execResult.getMessage();
            if (testStatus == JudgeStatus.ACCEPTED) {
                CheckResult checkResult = hasCustomChecker
                        ? sandbox.runPythonChecker(boxId, task.getCustomCheckerCode(), resolvedTestCase.input(), resolvedTestCase.expectedOutput(), execResult.getStdout())
                        : standardChecker.check(resolvedTestCase.input(), resolvedTestCase.expectedOutput(), execResult.getStdout());
                testStatus = checkResult.getStatus();
                message = checkResult.getMessage();
            }

            TestCaseResult testCaseResult = TestCaseResult.builder()
                    .status(testStatus)
                    .timeMs(execResult.getTimeMs())
                    .memoryKb(execResult.getMemoryKb())
                    .actualOutput(outputPreview(testStatus, execResult.getStdout()))
                    .message(truncateUtf8(message, resultMessagePreviewBytes))
                    .build();

            results.add(testCaseResult);
            maxTime = Math.max(maxTime, execResult.getTimeMs() != null ? execResult.getTimeMs() : 0);
            maxMemory = Math.max(maxMemory, execResult.getMemoryKb() != null ? execResult.getMemoryKb() : 0);

            if (testStatus != JudgeStatus.ACCEPTED) {
                overallStatus = testStatus;
                break;
            }
        }

        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .overallStatus(overallStatus)
                .testCaseResults(results)
                .maxTimeMs(maxTime)
                .maxMemoryKb(maxMemory)
                .build();
    }

    private String validateTask(ComputingTask task) {
        if (task == null) {
            return "ComputingTask is required";
        }
        if (task.getSubmissionId() == null || task.getSubmissionId().isBlank()) {
            return "submissionId is required";
        }
        if (task.getUserCode() == null) {
            return "userCode is required";
        }
        if (task.getUserCode().getLanguage() == null) {
            return "userCode.language is required";
        }
        if (task.getUserCode().getSourceCode() == null) {
            return "userCode.sourceCode is required";
        }
        if (task.getTestCases() == null || task.getTestCases().isEmpty()) {
            return "ComputingTask must contain at least one test case";
        }
        for (int index = 0; index < task.getTestCases().size(); index++) {
            TestCase testCase = task.getTestCases().get(index);
            if (testCase == null) {
                return "testCases[" + index + "] is required";
            }
            if (testCase.getInputFile() == null || testCase.getInputFile().getObjectKey() == null
                    || testCase.getInputFile().getObjectKey().isBlank()) {
                return "testCases[" + index + "].inputFile.objectKey is required";
            }
            if (testCase.getExpectedOutputFile() == null || testCase.getExpectedOutputFile().getObjectKey() == null
                    || testCase.getExpectedOutputFile().getObjectKey().isBlank()) {
                return "testCases[" + index + "].expectedOutputFile.objectKey is required";
            }
        }
        if (isNotPositive(task.getTimeLimitMs())) {
            return "timeLimitMs must be positive";
        }
        if (isNotPositive(task.getMemoryLimitKb())) {
            return "memoryLimitKb must be positive";
        }
        if (isNotPositive(task.getOutputLimitBytes())) {
            return "outputLimitBytes must be positive";
        }
        return null;
    }

    private boolean isNotPositive(Long value) {
        return value != null && value <= 0;
    }

    private JudgeResult systemErrorResult(String submissionId, String message) {
        return JudgeResult.builder()
                .submissionId(submissionId)
                .overallStatus(JudgeStatus.SYSTEM_ERROR)
                .compilationError(truncateUtf8(message, resultMessagePreviewBytes))
                .build();
    }

    private String outputPreview(JudgeStatus status, String stdout) {
        if (status == JudgeStatus.ACCEPTED) {
            return null;
        }
        return truncateUtf8(stdout, resultOutputPreviewBytes);
    }

    private String truncateUtf8(String value, int maxBytes) {
        if (value == null || maxBytes <= 0) {
            return null;
        }
        if (value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return value;
        }

        String suffix = "\n[truncated]";
        int suffixBytes = suffix.getBytes(StandardCharsets.UTF_8).length;
        int contentBudget = maxBytes - suffixBytes;
        if (contentBudget <= 0) {
            suffix = "";
            contentBudget = maxBytes;
        }

        StringBuilder builder = new StringBuilder();
        int usedBytes = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            String next = new String(Character.toChars(codePoint));
            int nextBytes = next.getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + nextBytes > contentBudget) {
                break;
            }
            builder.append(next);
            usedBytes += nextBytes;
            offset += Character.charCount(codePoint);
        }
        return builder.append(suffix).toString();
    }
}
