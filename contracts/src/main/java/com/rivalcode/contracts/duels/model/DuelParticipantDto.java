package com.rivalcode.contracts.duels.model;

import com.rivalcode.contracts.duels.enums.DuelOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelParticipantDto {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Integer ratingBefore;
    private Integer ratingAfter;
    private DuelOutcome outcome;
    private String acceptedSubmissionId;
}
