package com.vinogradov.onlinejudge.service.impl;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import com.vinogradov.onlinejudge.service.Checker;
import org.springframework.stereotype.Component;

@Component
public class StandardChecker implements Checker {
    @Override
    public JudgeStatus check(String actual, String expected) {
        if (actual == null || expected == null) return JudgeStatus.WRONG_ANSWER;

        String normActual = actual.trim().replaceAll("\\s+", " ");
        String normExpected = expected.trim().replaceAll("\\s+", " ");
        
        return normActual.equals(normExpected) ? JudgeStatus.ACCEPTED : JudgeStatus.WRONG_ANSWER;
    }
}
