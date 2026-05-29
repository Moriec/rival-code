package com.rivalcode.contracts.duels.model;

import com.rivalcode.contracts.duels.enums.RatingChangeReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingChangeDto {
    private String userId;
    private String duelId;
    private String seasonId;
    private Integer oldRating;
    private Integer newRating;
    private Integer delta;
    private RatingChangeReason reason;
    private Instant changedAt;
}
