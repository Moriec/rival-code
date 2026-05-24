package com.vinogradov.contracts.duels.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelProblemPoolDto {
    private String poolId;
    private String name;
    private String seasonId;
    private String presetId;
    private List<String> problemIds;
    private Boolean active;
}
