package com.rivalcode.contracts.submissions.model;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSubmissionVerdict {
    private String submissionId;
    private JudgeStatus overallStatus;
    private Long maxTimeMs;
    private Long maxMemoryKb;
    private Integer passedTests;
    private Integer totalTests;
    private String compilationError;
    private List<PublicTestCaseResult> visibleTests;
}
