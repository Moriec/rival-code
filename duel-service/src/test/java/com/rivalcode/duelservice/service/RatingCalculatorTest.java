package com.rivalcode.duelservice.service;

import com.rivalcode.contracts.duels.enums.DuelOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RatingCalculatorTest {

    private final RatingCalculator calculator = new RatingCalculator();

    @Test
    void returnsSymmetricDeltaForWinAndLoss() {
        ReflectionTestUtils.setField(calculator, "winRatingDelta", 25);

        assertThat(calculator.delta(DuelOutcome.WIN)).isEqualTo(25);
        assertThat(calculator.delta(DuelOutcome.LOSS)).isEqualTo(-25);
    }

    @Test
    void returnsZeroForDrawAndCancelled() {
        ReflectionTestUtils.setField(calculator, "winRatingDelta", 25);

        assertThat(calculator.delta(DuelOutcome.DRAW)).isZero();
        assertThat(calculator.delta(DuelOutcome.CANCELLED)).isZero();
    }
}
