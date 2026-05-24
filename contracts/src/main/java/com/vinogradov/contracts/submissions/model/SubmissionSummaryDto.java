package com.vinogradov.contracts.submissions.model;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;
import com.vinogradov.contracts.submissions.enums.SubmissionMode;
import com.vinogradov.contracts.submissions.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionSummaryDto {
    private String submissionId;
    private String userId;
    private String problemId;
    private String problemVersionId;
    private String duelId;
    private SubmissionMode mode;
    private ProgrammingLanguages language;
    private SubmissionStatus status;
    private JudgeStatus overallStatus;
    private Boolean accepted;
    private Long maxTimeMs;
    private Long maxMemoryKb;
    private Instant createdAt;
    private Instant judgedAt;
}
