package com.rivalcode.problemservice.service;

import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionRequest;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionResponse;
import com.rivalcode.problemservice.model.Problem;
import com.rivalcode.problemservice.model.ProblemVersion;
import com.rivalcode.problemservice.model.TestSuite;
import com.rivalcode.problemservice.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuelProblemSelectorTest {

    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private ProblemVersionRepository problemVersionRepository;
    @Mock
    private ProblemExampleRepository problemExampleRepository;
    @Mock
    private ProblemTagRepository problemTagRepository;
    @Mock
    private TestSuiteRepository testSuiteRepository;
    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private DuelProblemSelector selector;

    @Test
    void selectsOnlyPublishedProblemWithRequestedDifficulty() {
        UUID easyId = UUID.randomUUID();
        UUID hardId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Problem easy = problem(easyId, "cf-easy", "Easy Task", ProblemDifficulty.EASY, "PUBLISHED");
        Problem hard = problem(hardId, "cf-hard", "Hard Task", ProblemDifficulty.HARD, "PUBLISHED");
        ProblemVersion version = version(easyId, versionId);

        when(problemRepository.findAll()).thenReturn(List.of(hard, easy));
        when(problemVersionRepository.findByProblemIdAndActiveTrue(easyId)).thenReturn(Optional.of(version));
        when(problemExampleRepository.findByProblemVersionIdOrderByOrderNo(versionId)).thenReturn(List.of());
        when(problemTagRepository.findByProblem_ProblemId(easyId)).thenReturn(List.of());
        when(testSuiteRepository.findByProblemVersionId(versionId)).thenReturn(Optional.of(TestSuite.builder()
                .problemVersionId(versionId)
                .objectKey("s3://olimp-tests/easy/")
                .visibleSampleTestsCount(0)
                .build()));

        DuelProblemSelectionResponse response = selector.select(DuelProblemSelectionRequest.builder()
                .difficulties(List.of(ProblemDifficulty.EASY))
                .build());

        assertThat(response.getProblemId()).isEqualTo(easyId.toString());
        assertThat(response.getTitle()).isEqualTo("Easy Task");
    }

    @Test
    void throwsNotFoundWhenRequestedDifficultyHasNoPublishedProblems() {
        when(problemRepository.findAll()).thenReturn(List.of(
                problem(UUID.randomUUID(), "cf-easy", "Easy Task", ProblemDifficulty.EASY, "PUBLISHED")
        ));

        assertThatThrownBy(() -> selector.select(DuelProblemSelectionRequest.builder()
                .difficulties(List.of(ProblemDifficulty.HARD))
                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Problem problem(
            UUID problemId,
            String slug,
            String title,
            ProblemDifficulty difficulty,
            String status
    ) {
        return Problem.builder()
                .problemId(problemId)
                .slug(slug)
                .title(title)
                .difficulty(difficulty.name())
                .status(status)
                .build();
    }

    private ProblemVersion version(UUID problemId, UUID versionId) {
        return ProblemVersion.builder()
                .problemId(problemId)
                .problemVersionId(versionId)
                .statement("statement")
                .checkerType(CheckerType.STANDARD.name())
                .timeLimitMs(1000L)
                .memoryLimitKb(262144L)
                .outputLimitBytes(65536L)
                .active(true)
                .build();
    }
}
