package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.exception.CompilationFailedException;
import com.rivalcode.onlinejudge.model.CheckResult;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;
import com.rivalcode.onlinejudge.service.Checker;
import com.rivalcode.onlinejudge.service.Sandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultJudgeServiceTest {

    @Mock private Sandbox sandbox;
    @Mock private Checker checker;
    private DefaultJudgeService judgeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        judgeService = new DefaultJudgeService(sandbox, checker);
    }

    @Test
    void shouldHandleCompilationError() throws Exception {
        when(sandbox.compile(anyInt(), any())).thenThrow(new CompilationFailedException("Syntax Error"));

        JudgeResult result = judgeService.judge(validTask("sub-1", List.of(new TestCase("", "42"))), 0);

        assertEquals(JudgeStatus.COMPILATION_ERROR, result.getOverallStatus());
        assertEquals("Syntax Error", result.getCompilationError());
    }

    @Test
    void shouldReturnSystemErrorForInvalidTask() throws Exception {
        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-invalid")
                .userCode(new UserCode("code", ProgrammingLanguages.JAVA))
                .testCases(List.of())
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.SYSTEM_ERROR, result.getOverallStatus());
        verify(sandbox, never()).compile(anyInt(), any());
    }

    @Test
    void shouldReturnAcceptedWhenAllTestsPass() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(acceptedExecution("42"));
        when(checker.check(any(), any(), any())).thenReturn(CheckResult.builder().status(JudgeStatus.ACCEPTED).build());

        JudgeResult result = judgeService.judge(validTask("sub-accepted", List.of(
                new TestCase("in1", "42"),
                new TestCase("in2", "42")
        )), 0);

        assertEquals(JudgeStatus.ACCEPTED, result.getOverallStatus());
        assertEquals(2, result.getTestCaseResults().size());
        assertEquals(100L, result.getMaxTimeMs());
        assertEquals(1024L, result.getMaxMemoryKb());
    }

    @Test
    void shouldUseDefaultLimitsWhenTaskDoesNotContainLimits() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(acceptedExecution("42"));
        when(checker.check(any(), any(), any())).thenReturn(CheckResult.builder().status(JudgeStatus.ACCEPTED).build());

        JudgeResult result = judgeService.judge(validTask("sub-default-limits", List.of(new TestCase("", "42"))), 0);

        assertEquals(JudgeStatus.ACCEPTED, result.getOverallStatus());
        verify(sandbox).run(eq(0), eq(mockCompilation), eq(""), eq(2000L), eq(65536L), eq(1048576L));
    }

    @Test
    void shouldReturnWrongAnswerWhenCheckerFails() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(acceptedExecution("wrong"));
        when(checker.check(eq("in"), eq("42"), eq("wrong")))
                .thenReturn(CheckResult.builder().status(JudgeStatus.WRONG_ANSWER).message("Different tokens").build());

        JudgeResult result = judgeService.judge(validTask("sub-wa", List.of(new TestCase("in", "42"))), 0);

        assertEquals(JudgeStatus.WRONG_ANSWER, result.getOverallStatus());
        assertEquals("Different tokens", result.getTestCaseResults().get(0).getMessage());
    }

    @Test
    void shouldRunPythonCustomCheckerWhenCheckerCodeIsPresent() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(acceptedExecution("41 1"));
        when(sandbox.runPythonChecker(eq(0), eq("checker code"), eq("in"), eq("42"), eq("41 1")))
                .thenReturn(CheckResult.builder().status(JudgeStatus.ACCEPTED).build());

        ComputingTask task = validTask("sub-custom", List.of(new TestCase("in", "42")));
        task.setCustomCheckerCode("checker code");
        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.ACCEPTED, result.getOverallStatus());
        verify(checker, never()).check(any(), any(), any());
    }

    @Test
    void shouldStopWithSystemErrorWhenCustomCheckerFails() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(acceptedExecution("42"));
        when(sandbox.runPythonChecker(anyInt(), anyString(), any(), any(), any()))
                .thenReturn(CheckResult.builder().status(JudgeStatus.SYSTEM_ERROR).message("checker timed out").build());

        ComputingTask task = validTask("sub-custom-error", List.of(new TestCase("in", "42")));
        task.setCustomCheckerCode("checker code");
        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.SYSTEM_ERROR, result.getOverallStatus());
        assertEquals("checker timed out", result.getTestCaseResults().get(0).getMessage());
    }

    @Test
    void shouldReturnMemoryLimitExceeded() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(ExecutionResult.builder()
                        .status(JudgeStatus.MEMORY_LIMIT_EXCEEDED)
                        .memoryKb(2048L)
                        .build());

        JudgeResult result = judgeService.judge(validTask("sub-mle", List.of(new TestCase("in", "out"))), 0);

        assertEquals(JudgeStatus.MEMORY_LIMIT_EXCEEDED, result.getOverallStatus());
        assertEquals(2048L, result.getMaxMemoryKb());
    }

    @Test
    void shouldStopAtFirstFailure() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(anyInt(), any())).thenReturn(mockCompilation);
        when(sandbox.run(anyInt(), any(), eq("in1"), anyLong(), anyLong(), anyLong()))
                .thenReturn(acceptedExecution("out1"));
        when(sandbox.run(anyInt(), any(), eq("in2"), anyLong(), anyLong(), anyLong()))
                .thenReturn(ExecutionResult.builder().status(JudgeStatus.TIME_LIMIT_EXCEEDED).build());
        when(checker.check(any(), any(), any())).thenReturn(CheckResult.builder().status(JudgeStatus.ACCEPTED).build());

        JudgeResult result = judgeService.judge(validTask("sub-2", List.of(
                new TestCase("in1", "out1"),
                new TestCase("in2", "out2"),
                new TestCase("in3", "out3")
        )), 0);

        assertEquals(JudgeStatus.TIME_LIMIT_EXCEEDED, result.getOverallStatus());
        assertEquals(2, result.getTestCaseResults().size());
        verify(sandbox, times(2)).run(anyInt(), any(), any(), anyLong(), anyLong(), anyLong());
    }

    private ComputingTask validTask(String submissionId, List<TestCase> testCases) {
        return ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(new UserCode("public class Main {}", ProgrammingLanguages.JAVA))
                .testCases(testCases)
                .build();
    }

    private ExecutionResult acceptedExecution(String stdout) {
        return ExecutionResult.builder()
                .status(JudgeStatus.ACCEPTED)
                .stdout(stdout)
                .timeMs(100L)
                .memoryKb(1024L)
                .build();
    }
}
