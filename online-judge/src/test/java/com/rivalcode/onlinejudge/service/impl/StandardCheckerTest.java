package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StandardCheckerTest {

    private final StandardChecker checker = new StandardChecker();

    @Test
    void shouldAcceptExactMatch() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("", "42", "42\n").getStatus());
    }

    @Test
    void shouldAcceptWithDifferentTrailingWhitespace() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("", "42", "42  \n\n").getStatus());
    }

    @Test
    void shouldAcceptWithNormalizedInternalSpaces() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("", "1 2 3", "1   2 \n 3").getStatus());
    }

    @Test
    void shouldRejectWrongAnswer() {
        assertEquals(JudgeStatus.WRONG_ANSWER, checker.check("", "42", "43").getStatus());
    }

    @Test
    void shouldHandleEmptyOutputs() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("", "", "").getStatus());
        assertEquals(JudgeStatus.WRONG_ANSWER, checker.check("", "", "something").getStatus());
    }
}
