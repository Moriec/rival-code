package com.rivalcode.contracts.duels.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private String userId;
    private String username;
    private String avatarUrl;
    private Integer rating;
    private Integer rank;
    private Integer wins;
    private Integer losses;
    private Integer draws;
}
