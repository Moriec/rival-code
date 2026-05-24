package com.rivalcode.contracts.submissions.model;

import com.rivalcode.contracts.problems.enums.CheckerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionExecutionContext {
    private String submissionId;
    private String problemVersionId;
    private Long timeLimitMs;
    private Long memoryLimitKb;
    private CheckerType checkerType;
    private String testArchiveObjectKey;
    private Integer visibleSampleTestsCount;
    private Boolean storeFullJudgeLog;
    private Boolean ratedMode;
}
