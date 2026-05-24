package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;
import com.rivalcode.onlinejudge.service.Checker;
import com.rivalcode.onlinejudge.service.Sandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        when(sandbox.compile(any())).thenThrow(new RuntimeException("Syntax Error"));

        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-1")
                .userCode(new UserCode("invalid", null))
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.COMPILATION_ERROR, result.getOverallStatus());
        assertEquals("Syntax Error", result.getCompilationError());
    }

    @Test
    void shouldReturnAcceptedWhenAllTestsPass() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(any())).thenReturn(mockCompilation);

        when(sandbox.run(anyInt(), any(), anyString(), any(), any()))
                .thenReturn(ExecutionResult.builder()
                        .status(JudgeStatus.ACCEPTED)
                        .stdout("42")
                        .timeMs(100L)
                        .memoryKb(1024L)
                        .build());

        when(checker.check(anyString(), anyString())).thenReturn(JudgeStatus.ACCEPTED);

        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-accepted")
                .testCases(List.of(
                        new TestCase("in1", "42"),
                        new TestCase("in2", "42")
                ))
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.ACCEPTED, result.getOverallStatus());
        assertEquals(2, result.getTestCaseResults().size());
        assertEquals(100L, result.getMaxTimeMs());
        assertEquals(1024L, result.getMaxMemoryKb());
    }

    @Test
    void shouldUseDefaultLimitsWhenTaskDoesNotContainLimits() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(any())).thenReturn(mockCompilation);

        when(sandbox.run(anyInt(), any(), anyString(), any(), any()))
                .thenReturn(ExecutionResult.builder()
                        .status(JudgeStatus.ACCEPTED)
                        .stdout("42")
                        .timeMs(100L)
                        .memoryKb(1024L)
                        .build());

        when(checker.check(anyString(), anyString())).thenReturn(JudgeStatus.ACCEPTED);

        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-default-limits")
                .testCases(List.of(new TestCase("", "42")))
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.ACCEPTED, result.getOverallStatus());
        verify(sandbox).run(eq(0), eq(mockCompilation), eq(""), eq(2000L), eq(65536L));
    }

    @Test
    void shouldReturnWrongAnswerWhenCheckerFails() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(any())).thenReturn(mockCompilation);

        when(sandbox.run(anyInt(), any(), anyString(), any(), any()))
                .thenReturn(ExecutionResult.builder()
                        .status(JudgeStatus.ACCEPTED)
                        .stdout("wrong")
                        .build());

        when(checker.check(eq("wrong"), eq("42"))).thenReturn(JudgeStatus.WRONG_ANSWER);

        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-wa")
                .testCases(List.of(new TestCase("in", "42")))
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.WRONG_ANSWER, result.getOverallStatus());
        assertEquals(JudgeStatus.WRONG_ANSWER, result.getTestCaseResults().get(0).getStatus());
    }

    @Test
    void shouldReturnMemoryLimitExceeded() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(any())).thenReturn(mockCompilation);

        when(sandbox.run(anyInt(), any(), anyString(), any(), any()))
                .thenReturn(ExecutionResult.builder()
                        .status(JudgeStatus.MEMORY_LIMIT_EXCEEDED)
                        .memoryKb(2048L)
                        .build());

        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-mle")
                .testCases(List.of(new TestCase("in", "out")))
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.MEMORY_LIMIT_EXCEEDED, result.getOverallStatus());
        assertEquals(2048L, result.getMaxMemoryKb());
    }

    @Test
    void shouldStopAtFirstFailure() throws Exception {
        CompilationResult mockCompilation = mock(CompilationResult.class);
        when(sandbox.compile(any())).thenReturn(mockCompilation);

        when(sandbox.run(anyInt(), any(), eq("in1"), any(), any()))
                .thenReturn(ExecutionResult.builder().status(JudgeStatus.ACCEPTED).stdout("out1").build());
        when(sandbox.run(anyInt(), any(), eq("in2"), any(), any()))
                .thenReturn(ExecutionResult.builder().status(JudgeStatus.TIME_LIMIT_EXCEEDED).build());

        when(checker.check(any(), any())).thenReturn(JudgeStatus.ACCEPTED);

        ComputingTask task = ComputingTask.builder()
                .submissionId("sub-2")
                .testCases(List.of(
                        new TestCase("in1", "out1"),
                        new TestCase("in2", "out2")
                ))
                .build();

        JudgeResult result = judgeService.judge(task, 0);

        assertEquals(JudgeStatus.TIME_LIMIT_EXCEEDED, result.getOverallStatus());
        assertEquals(2, result.getTestCaseResults().size());
        verify(sandbox, times(2)).run(anyInt(), any(), any(), any(), any());
    }
}
