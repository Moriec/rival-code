package com.rivalcode.contracts.duels.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDuelProfileDto {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Integer rating;
    private Integer rank;
    private Integer wins;
    private Integer losses;
    private Integer draws;
    private Integer solvedProblems;
    private List<RecentDuelDto> recentDuels;
}
