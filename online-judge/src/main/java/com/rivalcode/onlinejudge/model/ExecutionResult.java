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
public class ExecutionResult {
    private String stdout;
    private String stderr;
    private int exitCode;
    private Long timeMs;
    private Long memoryKb;
    private JudgeStatus status;
}
