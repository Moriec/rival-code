package com.vinogradov.contracts.problems.model;

import com.vinogradov.contracts.problems.enums.ProblemDifficulty;
import com.vinogradov.contracts.problems.enums.ProblemStatus;
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
public class ProblemSummaryDto {
    private String problemId;
    private String slug;
    private String title;
    private ProblemDifficulty difficulty;
    private ProblemStatus status;
    private List<TagDto> tags;
    private Long acceptedCount;
    private Long attemptsCount;
    private Instant publishedAt;
}
