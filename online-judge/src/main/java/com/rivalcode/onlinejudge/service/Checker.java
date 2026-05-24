package com.rivalcode.onlinejudge.service;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;

public interface Checker {
    /**
     * Compares actual output with expected output.
     */
    JudgeStatus check(String actual, String expected);
}
