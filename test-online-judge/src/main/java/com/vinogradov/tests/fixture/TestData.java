package com.vinogradov.tests.fixture;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import com.vinogradov.contracts.submissions.model.ComputingTask;
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
