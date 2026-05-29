package com.rivalcode.contracts.problems.model;

import com.rivalcode.contracts.problems.enums.CheckerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemExecutionContextDto {
    private String problemId;
    private String problemVersionId;
    private CheckerType checkerType;
    private ProblemLimitsDto limits;
    private String testArchiveObjectKey;
    private Integer visibleSampleTestsCount;
    private String customCheckerObjectKey;
}
