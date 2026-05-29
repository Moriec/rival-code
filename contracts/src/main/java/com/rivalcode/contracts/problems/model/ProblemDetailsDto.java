package com.rivalcode.contracts.problems.model;

import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.contracts.problems.enums.ProblemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailsDto {
    private String problemId;
    private String problemVersionId;
    private String slug;
    private String title;
    private String statement;
    private String inputSpec;
    private String outputSpec;
    private ProblemDifficulty difficulty;
    private ProblemStatus status;
    private CheckerType checkerType;
    private ProblemLimitsDto limits;
    private List<TagDto> tags;
    private List<ProblemExampleDto> examples;
    private Instant publishedAt;
    private Instant updatedAt;
}
