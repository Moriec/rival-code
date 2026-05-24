package com.vinogradov.onlinejudge.service.impl;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import com.vinogradov.contracts.submissionResult.model.JudgeResult;
import com.vinogradov.contracts.submissionResult.model.TestCaseResult;
import com.vinogradov.contracts.submissions.model.ComputingTask;
import com.vinogradov.contracts.submissions.model.TestCase;
import com.vinogradov.onlinejudge.model.CompilationResult;
import com.vinogradov.onlinejudge.model.ExecutionResult;
import com.vinogradov.onlinejudge.service.Checker;
import com.vinogradov.onlinejudge.service.JudgeService;
import com.vinogradov.onlinejudge.service.Sandbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJudgeService implements JudgeService {

    private final Sandbox sandbox;
    private final Checker standardChecker;

    @Override
    public JudgeResult judge(ComputingTask task, int boxId) {
        log.info("Judging submission {} using box {}", task.getSubmissionId(), boxId);

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

        Checker checker = standardChecker;
        if (task.getCustomCheckerCode() != null && !task.getCustomCheckerCode().isEmpty()) {
            checker = createCustomChecker(task.getCustomCheckerCode());
        }

        List<TestCaseResult> results = new ArrayList<>();
        JudgeStatus overallStatus = JudgeStatus.ACCEPTED;
        long maxTime = 0;
        long maxMemory = 0;

        try {
            for (TestCase testCase : task.getTestCases()) {
                ExecutionResult execResult = sandbox.run(
                        boxId,
                        compilation,
                        testCase.getInput(),
                        task.getTimeLimitMs(),
                        task.getMemoryLimitKb()
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
