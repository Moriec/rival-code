package com.rivalcode.contracts.duels.model;

import com.rivalcode.contracts.duels.enums.DuelMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMatchmakingTicketRequest {
    private String userId;
    private String presetId;
    private DuelMode mode;
    private Integer currentRating;
}
