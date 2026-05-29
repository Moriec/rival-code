package com.rivalcode.tests.fixture;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissions.model.ComputingTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestData {
    private String submissionId;
    private ComputingTask task;
    private JudgeStatus expectedStatus;
}
