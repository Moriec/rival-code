package com.rivalcode.submissionservice.storage;

import com.rivalcode.contracts.submissions.model.TestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestSuiteObjectMapperTest {

    private final TestSuiteObjectMapper mapper = new TestSuiteObjectMapper();

    @Test
    void mapsSortedInputOutputPairs() {
        List<TestCase> testCases = mapper.toTestCases("olimp-tests", List.of(
                "2063/A/002.out",
                "2063/A/001.in",
                "2063/A/readme.txt",
                "2063/A/002.in",
                "2063/A/001.out"
        ));

        assertThat(testCases).hasSize(2);
        assertThat(testCases.get(0).getInputFile().getObjectKey()).isEqualTo("2063/A/001.in");
        assertThat(testCases.get(0).getExpectedOutputFile().getObjectKey()).isEqualTo("2063/A/001.out");
        assertThat(testCases.get(1).getInputFile().getObjectKey()).isEqualTo("2063/A/002.in");
        assertThat(testCases.get(1).getExpectedOutputFile().getObjectKey()).isEqualTo("2063/A/002.out");
    }

    @Test
    void rejectsIncompletePair() {
        assertThatThrownBy(() -> mapper.toTestCases("olimp-tests", List.of("2063/A/001.in")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Incomplete test pair");
    }
}
