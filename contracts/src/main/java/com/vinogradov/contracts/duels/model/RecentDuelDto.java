package com.vinogradov.contracts.duels.model;

import com.vinogradov.contracts.duels.enums.DuelMode;
import com.vinogradov.contracts.duels.enums.DuelOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentDuelDto {
    private String duelId;
    private String opponentUserId;
    private String opponentUsername;
    private String problemId;
    private String problemTitle;
    private DuelMode mode;
    private DuelOutcome outcome;
    private Integer ratingDelta;
    private Instant finishedAt;
}
