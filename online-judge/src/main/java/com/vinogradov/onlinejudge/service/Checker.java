package com.vinogradov.onlinejudge.service;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;

public interface Checker {
    /**
     * Compares actual output with expected output.
     */
    JudgeStatus check(String actual, String expected);
}
