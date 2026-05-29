package com.rivalcode.contracts.problems.model;

import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelProblemSelectionRequest {
    private String presetId;
    private String poolId;
    private List<String> userIds;
    private List<ProblemDifficulty> difficulties;
    private List<String> tagIds;
    private List<String> excludedProblemIds;
    private Boolean ratedMode;
}
