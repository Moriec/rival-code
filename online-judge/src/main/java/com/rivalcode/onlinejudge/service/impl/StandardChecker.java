package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.onlinejudge.model.CheckResult;
import com.rivalcode.onlinejudge.service.Checker;
import org.springframework.stereotype.Component;

@Component
public class StandardChecker implements Checker {
    @Override
    public CheckResult check(String input, String expectedOutput, String actualOutput) {
        if (actualOutput == null || expectedOutput == null) {
            return CheckResult.builder()
                    .status(JudgeStatus.WRONG_ANSWER)
                    .message("Output or expected output is missing")
                    .build();
        }

        String normActual = actualOutput.trim().replaceAll("\\s+", " ");
        String normExpected = expectedOutput.trim().replaceAll("\\s+", " ");
        
        return CheckResult.builder()
                .status(normActual.equals(normExpected) ? JudgeStatus.ACCEPTED : JudgeStatus.WRONG_ANSWER)
                .build();
    }
}
