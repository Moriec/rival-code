package com.rivalcode.onlinejudge.model;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {
    private JudgeStatus status;
    private String message;
}
