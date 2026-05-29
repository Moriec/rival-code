package com.rivalcode.problemservice.service;

import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.model.ProblemExecutionContextDto;
import com.rivalcode.problemservice.model.Problem;
import com.rivalcode.problemservice.model.ProblemVersion;
import com.rivalcode.problemservice.model.TestSuite;
import com.rivalcode.problemservice.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemServiceExecutionContextTest {

    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private ProblemVersionRepository problemVersionRepository;
    @Mock
    private ProblemExampleRepository problemExampleRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ProblemTagRepository problemTagRepository;
    @Mock
    private TestSuiteRepository testSuiteRepository;

    @InjectMocks
    private ProblemService problemService;

    @Test
    void returnsExecutionContextFromActiveVersionAndTestSuite() {
        UUID problemId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Problem problem = Problem.builder()
                .problemId(problemId)
                .build();
        ProblemVersion version = ProblemVersion.builder()
                .problemVersionId(versionId)
                .problemId(problemId)
                .checkerType(CheckerType.STANDARD.name())
                .timeLimitMs(2000L)
                .memoryLimitKb(262144L)
                .outputLimitBytes(65536L)
                .active(true)
                .build();
        TestSuite testSuite = TestSuite.builder()
                .problemVersionId(versionId)
                .objectKey("s3://olimp-tests/2063/A/")
                .visibleSampleTestsCount(0)
                .build();

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(problemVersionRepository.findByProblemIdAndActiveTrue(problemId)).thenReturn(Optional.of(version));
        when(testSuiteRepository.findByProblemVersionId(versionId)).thenReturn(Optional.of(testSuite));

        ProblemExecutionContextDto context = problemService.getExecutionContext(problemId, null);

        assertThat(context.getProblemId()).isEqualTo(problemId.toString());
        assertThat(context.getProblemVersionId()).isEqualTo(versionId.toString());
        assertThat(context.getCheckerType()).isEqualTo(CheckerType.STANDARD);
        assertThat(context.getLimits().getTimeLimitMs()).isEqualTo(2000L);
        assertThat(context.getLimits().getMemoryLimitKb()).isEqualTo(262144L);
        assertThat(context.getLimits().getOutputLimitBytes()).isEqualTo(65536L);
        assertThat(context.getTestArchiveObjectKey()).isEqualTo("s3://olimp-tests/2063/A/");
        assertThat(context.getVisibleSampleTestsCount()).isZero();
    }

    @Test
    void returnsCustomCheckerObjectKeyWhenProblemUsesCustomChecker() {
        UUID problemId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Problem problem = Problem.builder()
                .problemId(problemId)
                .build();
        ProblemVersion version = ProblemVersion.builder()
                .problemVersionId(versionId)
                .problemId(problemId)
                .checkerType(CheckerType.CUSTOM.name())
                .customCheckerObjectKey("s3://olimp-tests/2010/C2/checker.py")
                .timeLimitMs(2000L)
                .memoryLimitKb(262144L)
                .outputLimitBytes(65536L)
                .active(true)
                .build();
        TestSuite testSuite = TestSuite.builder()
                .problemVersionId(versionId)
                .objectKey("s3://olimp-tests/2010/C2/")
                .visibleSampleTestsCount(0)
                .build();

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(problemVersionRepository.findByProblemIdAndActiveTrue(problemId)).thenReturn(Optional.of(version));
        when(testSuiteRepository.findByProblemVersionId(versionId)).thenReturn(Optional.of(testSuite));

        ProblemExecutionContextDto context = problemService.getExecutionContext(problemId, null);

        assertThat(context.getCheckerType()).isEqualTo(CheckerType.CUSTOM);
        assertThat(context.getCustomCheckerObjectKey()).isEqualTo("s3://olimp-tests/2010/C2/checker.py");
    }
}
