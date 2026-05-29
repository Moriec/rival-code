package com.rivalcode.duelservice.service;

import com.rivalcode.contracts.duels.enums.DuelOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RatingCalculator {

    @Value("${app.duel.win-rating-delta:25}")
    private int winRatingDelta;

    public int delta(DuelOutcome outcome) {
        return switch (outcome) {
            case WIN -> winRatingDelta;
            case LOSS -> -winRatingDelta;
            case DRAW, CANCELLED -> 0;
        };
    }
}
