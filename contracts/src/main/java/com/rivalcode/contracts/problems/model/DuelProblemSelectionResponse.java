package com.rivalcode.contracts.problems.model;

import com.rivalcode.contracts.problems.enums.CheckerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelProblemSelectionResponse {
    private String problemId;
    private String problemVersionId;
    private String slug;
    private String title;
    private String statement;
    private CheckerType checkerType;
    private ProblemLimitsDto limits;
    private List<TagDto> tags;
    private List<ProblemExampleDto> examples;
    private String testArchiveObjectKey;
}
