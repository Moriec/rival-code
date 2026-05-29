package com.rivalcode.submissionservice.storage;

import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.contracts.submissions.model.TestCaseFileRef;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class TestSuiteObjectMapper {

    public List<TestCase> toTestCases(String bucket, List<String> objectNames) {
        Map<String, TestPair> pairs = new TreeMap<>(Comparator.naturalOrder());
        for (String objectName : objectNames) {
            addObject(pairs, objectName);
        }

        if (pairs.isEmpty()) {
            throw new IllegalStateException("Test suite has no .in/.out pairs");
        }

        return pairs.entrySet().stream()
                .map(entry -> toTestCase(bucket, entry.getKey(), entry.getValue()))
                .toList();
    }

    private void addObject(Map<String, TestPair> pairs, String objectName) {
        if (objectName.endsWith(".in")) {
            String base = objectName.substring(0, objectName.length() - 3);
            pairs.computeIfAbsent(base, ignored -> new TestPair()).inputObjectKey = objectName;
        } else if (objectName.endsWith(".out")) {
            String base = objectName.substring(0, objectName.length() - 4);
            pairs.computeIfAbsent(base, ignored -> new TestPair()).expectedObjectKey = objectName;
        }
    }

    private TestCase toTestCase(String bucket, String base, TestPair pair) {
        if (pair.inputObjectKey == null || pair.expectedObjectKey == null) {
            throw new IllegalStateException("Incomplete test pair for " + bucket + "/" + base);
        }

        return TestCase.builder()
                .inputFile(TestCaseFileRef.builder()
                        .bucket(bucket)
                        .objectKey(pair.inputObjectKey)
                        .build())
                .expectedOutputFile(TestCaseFileRef.builder()
                        .bucket(bucket)
                        .objectKey(pair.expectedObjectKey)
                        .build())
                .build();
    }

    private static final class TestPair {
        private String inputObjectKey;
        private String expectedObjectKey;
    }
}
