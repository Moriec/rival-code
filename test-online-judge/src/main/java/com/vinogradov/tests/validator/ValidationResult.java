package com.vinogradov.tests.validator;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {
    private boolean passed;
    private String message;
    private JudgeStatus expectedStatus;
    private JudgeStatus actualStatus;
}
