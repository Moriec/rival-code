package com.rivalcode.tests.validator;

import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.tests.fixture.TestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResultValidator {

    public ValidationResult validate(JudgeResult result, TestData expectedData) {
        ValidationResult validationResult = ValidationResult.builder()
                .expectedStatus(expectedData.getExpectedStatus())
                .actualStatus(result.getOverallStatus())
                .build();

        if (result.getOverallStatus() == expectedData.getExpectedStatus()) {
            validationResult.setPassed(true);
            validationResult.setMessage("PASSED");
            return validationResult;
        }

        validationResult.setPassed(false);
        validationResult.setMessage(String.format(
                "Expected status %s but got %s. Submission: %s",
                expectedData.getExpectedStatus(),
                result.getOverallStatus(),
                result.getSubmissionId()
        ));

        if (result.getCompilationError() != null) {
            validationResult.setMessage(validationResult.getMessage() +
                    "\nCompilation Error: " + result.getCompilationError());
        }

        return validationResult;
    }

    public boolean isPass(ValidationResult validationResult) {
        return validationResult.isPassed();
    }
}
