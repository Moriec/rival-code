package com.rivalcode.submissionservice.storage;

import com.rivalcode.contracts.submissions.model.TestCase;

import java.util.List;

public record ResolvedTestSuite(
        String bucket,
        String prefix,
        List<TestCase> testCases
) {
}
