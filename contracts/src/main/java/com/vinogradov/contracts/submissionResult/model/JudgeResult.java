package com.vinogradov.contracts.submissionResult.model;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {
    private String submissionId;
    private JudgeStatus overallStatus;
    private Long maxTimeMs;
    private Long maxMemoryKb;
    private List<TestCaseResult> testCaseResults;
    private String compilationError;
}
