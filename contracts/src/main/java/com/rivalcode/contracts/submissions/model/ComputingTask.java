package com.rivalcode.contracts.submissions.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputingTask {
    private String submissionId;
    private UserCode userCode;
    private List<TestCase> testCases;
    private Long timeLimitMs;
    private Long memoryLimitKb;
    private String customCheckerCode;
}
