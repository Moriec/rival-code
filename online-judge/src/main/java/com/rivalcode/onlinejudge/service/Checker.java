package com.rivalcode.onlinejudge.service;

import com.rivalcode.onlinejudge.model.CheckResult;

public interface Checker {
    /**
     * Checks solution output for a single test case.
     */
    CheckResult check(String input, String expectedOutput, String actualOutput);
}
