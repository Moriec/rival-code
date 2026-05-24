package com.vinogradov.contracts.submissions.model;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import com.vinogradov.contracts.submissions.enums.SubmissionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEvaluatedEvent {
    private String submissionId;
    private String userId;
    private String duelId;
    private String problemId;
    private String problemVersionId;
    private SubmissionMode mode;
    private JudgeStatus overallStatus;
    private Boolean accepted;
    private Long maxTimeMs;
    private Long maxMemoryKb;
    private Integer passedTests;
    private Integer totalTests;
}
