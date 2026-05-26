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
import com.rivalcode.onlinejudge.service.Checker;
import com.rivalcode.onlinejudge.service.JudgeService;
import com.rivalcode.onlinejudge.service.Sandbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJudgeService implements JudgeService {

    private final Sandbox sandbox;
    private final Checker standardChecker;

    @Value("${app.sandbox.default-time-limit-ms:2000}")
    private Long defaultTimeLimitMs = 2000L;

    @Value("${app.sandbox.default-memory-limit-kb:65536}")
    private Long defaultMemoryLimitKb = 65536L;

    @Value("${app.sandbox.default-output-limit-bytes:1048576}")
    private Long defaultOutputLimitBytes = 1048576L;

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
                    .compilationError(e.getMessage())
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
            ExecutionResult execResult = sandbox.run(
                    boxId,
                    compilation,
                    testCase.getInput(),
                    timeLimitMs,
                    memoryLimitKb,
                    outputLimitBytes
            );

            JudgeStatus testStatus = execResult.getStatus();
            String message = execResult.getMessage();
            if (testStatus == JudgeStatus.ACCEPTED) {
                CheckResult checkResult = hasCustomChecker
                        ? sandbox.runPythonChecker(boxId, task.getCustomCheckerCode(), testCase.getInput(), testCase.getExpectedOutput(), execResult.getStdout())
                        : standardChecker.check(testCase.getInput(), testCase.getExpectedOutput(), execResult.getStdout());
                testStatus = checkResult.getStatus();
                message = checkResult.getMessage();
            }

            TestCaseResult testCaseResult = TestCaseResult.builder()
                    .status(testStatus)
                    .timeMs(execResult.getTimeMs())
                    .memoryKb(execResult.getMemoryKb())
                    .input(testCase.getInput())
                    .actualOutput(execResult.getStdout())
                    .expectedOutput(testCase.getExpectedOutput())
                    .message(message)
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
            if (testCase.getExpectedOutput() == null) {
                return "testCases[" + index + "].expectedOutput is required";
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
                .compilationError(message)
                .build();
    }
}
