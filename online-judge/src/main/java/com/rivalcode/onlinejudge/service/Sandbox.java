package com.rivalcode.onlinejudge.service;

import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.model.CheckResult;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;

public interface Sandbox {
    CompilationResult compile(int boxId, UserCode userCode) throws Exception;
    ExecutionResult run(int boxId, CompilationResult compilation, String input, Long timeLimitMs, Long memoryLimitKb, Long outputLimitBytes);
    CheckResult runPythonChecker(int boxId, String checkerCode, String input, String expectedOutput, String actualOutput);
    void cleanup(int boxId);
}
