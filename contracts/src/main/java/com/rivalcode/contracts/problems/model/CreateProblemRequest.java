package com.rivalcode.contracts.problems.model;

import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.contracts.problems.enums.ProblemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProblemRequest {
    private String slug;
    private String title;
    private String statement;
    private String inputSpec;
    private String outputSpec;
    private ProblemDifficulty difficulty;
    private ProblemStatus status;
    private CheckerType checkerType;
    private ProblemLimitsDto limits;
    private List<String> tagIds;
    private List<ProblemExampleDto> examples;
}
