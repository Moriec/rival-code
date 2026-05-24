package com.vinogradov.onlinejudge.service;

import com.vinogradov.contracts.submissions.model.UserCode;
import com.vinogradov.onlinejudge.model.CompilationResult;
import com.vinogradov.onlinejudge.model.ExecutionResult;

public interface Sandbox {
    CompilationResult compile(UserCode userCode) throws Exception;
    ExecutionResult run(int boxId, CompilationResult compilation, String input, Long timeLimitMs, Long memoryLimitKb);
    void cleanup(int boxId);
}
