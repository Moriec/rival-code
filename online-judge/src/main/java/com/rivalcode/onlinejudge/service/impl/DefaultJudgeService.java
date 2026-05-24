package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissionResult.model.TestCaseResult;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissions.model.TestCase;
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
import java.util.Collections;
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

    @Override
    public JudgeResult judge(ComputingTask task, int boxId) {
        log.info("Judging submission {} using box {}", task.getSubmissionId(), boxId);

        List<TestCase> testCases = task.getTestCases() != null ? task.getTestCases() : Collections.emptyList();
        Long timeLimitMs = task.getTimeLimitMs() != null ? task.getTimeLimitMs() : defaultTimeLimitMs;
        Long memoryLimitKb = task.getMemoryLimitKb() != null ? task.getMemoryLimitKb() : defaultMemoryLimitKb;

        CompilationResult compilation;
        try {
            compilation = sandbox.compile(task.getUserCode());
        } catch (Exception e) {
            return JudgeResult.builder()
                    .submissionId(task.getSubmissionId())
                    .overallStatus(JudgeStatus.COMPILATION_ERROR)
                    .compilationError(e.getMessage())
                    .build();
        }

        if (testCases.isEmpty()) {
            log.warn("Submission {} has no test cases", task.getSubmissionId());
            return JudgeResult.builder()
                    .submissionId(task.getSubmissionId())
                    .overallStatus(JudgeStatus.SYSTEM_ERROR)
                    .compilationError("ComputingTask must contain at least one test case")
                    .build();
        }

        Checker checker = standardChecker;
        if (task.getCustomCheckerCode() != null && !task.getCustomCheckerCode().isEmpty()) {
            checker = createCustomChecker(task.getCustomCheckerCode());
        }

        List<TestCaseResult> results = new ArrayList<>();
        JudgeStatus overallStatus = JudgeStatus.ACCEPTED;
        long maxTime = 0;
        long maxMemory = 0;

        try {
            for (TestCase testCase : testCases) {
                ExecutionResult execResult = sandbox.run(
                        boxId,
                        compilation,
                        testCase.getInput(),
                        timeLimitMs,
                        memoryLimitKb
                );

                JudgeStatus testStatus = execResult.getStatus();
                if (testStatus == JudgeStatus.ACCEPTED) {
                    // If code ran OK, check output
                    testStatus = checker.check(execResult.getStdout(), testCase.getExpectedOutput());
                }

                TestCaseResult testCaseResult = TestCaseResult.builder()
                        .status(testStatus)
                        .timeMs(execResult.getTimeMs())
                        .memoryKb(execResult.getMemoryKb())
                        .actualOutput(execResult.getStdout())
                        .expectedOutput(testCase.getExpectedOutput())
                        .build();

                results.add(testCaseResult);

                if (testStatus != JudgeStatus.ACCEPTED) {
                    overallStatus = testStatus;
                    maxTime = Math.max(maxTime, execResult.getTimeMs() != null ? execResult.getTimeMs() : 0);
                    maxMemory = Math.max(maxMemory, execResult.getMemoryKb() != null ? execResult.getMemoryKb() : 0);
                    break; 
                }
                
                maxTime = Math.max(maxTime, execResult.getTimeMs() != null ? execResult.getTimeMs() : 0);
                maxMemory = Math.max(maxMemory, execResult.getMemoryKb() != null ? execResult.getMemoryKb() : 0);
            }
        } finally {
            sandbox.cleanup(boxId);
        }

        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .overallStatus(overallStatus)
                .testCaseResults(results)
                .maxTimeMs(maxTime)
                .maxMemoryKb(maxMemory)
                .build();
    }

    private Checker createCustomChecker(String checkerCode) {
        return (actual, expected) -> {
            log.info("Running custom checker...");
            return standardChecker.check(actual, expected);
        };
    }
}
