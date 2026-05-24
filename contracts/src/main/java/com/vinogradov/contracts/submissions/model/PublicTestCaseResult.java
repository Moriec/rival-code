package com.vinogradov.contracts.submissions.model;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTestCaseResult {
    private Integer orderNo;
    private JudgeStatus status;
    private Long timeMs;
    private Long memoryKb;
    private String message;
}
