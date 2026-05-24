package com.vinogradov.onlinejudge.service.impl;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StandardCheckerTest {

    private final StandardChecker checker = new StandardChecker();

    @Test
    void shouldAcceptExactMatch() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("42\n", "42"));
    }

    @Test
    void shouldAcceptWithDifferentTrailingWhitespace() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("42  \n\n", "42"));
    }

    @Test
    void shouldAcceptWithNormalizedInternalSpaces() {
        // "1 2 3" vs "1   2 \n 3"
        assertEquals(JudgeStatus.ACCEPTED, checker.check("1   2 \n 3", "1 2 3"));
    }

    @Test
    void shouldRejectWrongAnswer() {
        assertEquals(JudgeStatus.WRONG_ANSWER, checker.check("43", "42"));
    }

    @Test
    void shouldHandleEmptyOutputs() {
        assertEquals(JudgeStatus.ACCEPTED, checker.check("", ""));
        assertEquals(JudgeStatus.WRONG_ANSWER, checker.check("something", ""));
    }
}
