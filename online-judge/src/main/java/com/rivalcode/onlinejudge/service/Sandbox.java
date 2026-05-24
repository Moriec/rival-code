package com.rivalcode.onlinejudge.service;

import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;

public interface Sandbox {
    CompilationResult compile(UserCode userCode) throws Exception;
    ExecutionResult run(int boxId, CompilationResult compilation, String input, Long timeLimitMs, Long memoryLimitKb);
    void cleanup(int boxId);
}
