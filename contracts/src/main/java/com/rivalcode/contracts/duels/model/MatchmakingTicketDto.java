package com.rivalcode.contracts.duels.model;

import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.duels.enums.MatchmakingTicketStatus;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingTicketDto {
    private String ticketId;
    private String userId;
    private String presetId;
    private String matchedDuelId;
    private DuelMode mode;
    private ProblemDifficulty difficulty;
    private MatchmakingTicketStatus status;
    private Integer currentRating;
    private Instant createdAt;
    private Instant expiresAt;
}
