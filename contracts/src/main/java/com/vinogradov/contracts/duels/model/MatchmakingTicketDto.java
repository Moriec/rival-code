package com.vinogradov.contracts.duels.model;

import com.vinogradov.contracts.duels.enums.DuelMode;
import com.vinogradov.contracts.duels.enums.MatchmakingTicketStatus;
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
    private DuelMode mode;
    private MatchmakingTicketStatus status;
    private Integer currentRating;
    private Instant createdAt;
    private Instant expiresAt;
}
