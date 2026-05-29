package com.rivalcode.problemservice.service;

import com.rivalcode.problemservice.model.*;
import com.rivalcode.problemservice.repository.*;
import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DuelProblemSelector {

    private final ProblemRepository problemRepository;
    private final ProblemVersionRepository problemVersionRepository;
    private final ProblemExampleRepository problemExampleRepository;
    private final ProblemTagRepository problemTagRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TagRepository tagRepository;

    public DuelProblemSelectionResponse select(DuelProblemSelectionRequest request) {
        List<Problem> candidates = problemRepository.findAll().stream()
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .collect(Collectors.toList());

        // ЗАГОТОВКА: использование poolId и presetId из запроса
        // if (request.getPoolId() != null) {
        //     List<UUID> poolProblemIds = duelPoolClient.getProblemIds(request.getPoolId());
        //     Set<UUID> poolSet = new HashSet<>(poolProblemIds);
        //     candidates = candidates.stream().filter(p -> poolSet.contains(p.getProblemId())).collect(Collectors.toList());
        // }

        // ЗАГОТОВКА: учёт userIds для исключения уже решённых задач этими пользователями
        // if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
        //     Set<UUID> allSolved = submissionClient.getSolvedProblemsForUsers(request.getUserIds());
        //     candidates = candidates.stream().filter(p -> !allSolved.contains(p.getProblemId())).collect(Collectors.toList());

        if (request.getExcludedProblemIds() != null) {
            Set<UUID> excluded = request.getExcludedProblemIds().stream()
                    .map(UUID::fromString).collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(p -> !excluded.contains(p.getProblemId()))
                    .collect(Collectors.toList());
        }

        if (request.getDifficulties() != null && !request.getDifficulties().isEmpty()) {
            Set<String> diffs = request.getDifficulties().stream()
                    .map(Enum::name).collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(p -> diffs.contains(p.getDifficulty()))
                    .collect(Collectors.toList());
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<UUID> tagIdSet = request.getTagIds().stream()
                    .map(UUID::fromString).collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(p -> problemTagRepository.findByProblem_ProblemId(p.getProblemId()).stream()
                            .anyMatch(pt -> tagIdSet.contains(pt.getTag().getTagId())))
                    .collect(Collectors.toList());
        }

        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No suitable problem found for duel");
        }

        Problem selected = candidates.get(new Random().nextInt(candidates.size()));
        ProblemVersion version = problemVersionRepository.findByProblemIdAndActiveTrue(selected.getProblemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active version for selected problem"));

        List<ProblemExample> examples = problemExampleRepository.findByProblemVersionIdOrderByOrderNo(version.getProblemVersionId());
        List<ProblemTag> problemTags = problemTagRepository.findByProblem_ProblemId(selected.getProblemId());

        TestSuite testSuite = testSuiteRepository.findByProblemVersionId(version.getProblemVersionId()).orElse(null);

        return DuelProblemSelectionResponse.builder()
                .problemId(selected.getProblemId().toString())
                .problemVersionId(version.getProblemVersionId().toString())
                .slug(selected.getSlug())
                .title(selected.getTitle())
                .statement(version.getStatement())
                .checkerType(CheckerType.valueOf(version.getCheckerType()))
                .limits(ProblemLimitsDto.builder()
                        .timeLimitMs(version.getTimeLimitMs())
                        .memoryLimitKb(version.getMemoryLimitKb())
                        .outputLimitBytes(version.getOutputLimitBytes())
                        .build())
                .tags(problemTags.stream().map(pt -> {
                    Tag tag = pt.getTag();
                    return TagDto.builder()
                            .tagId(tag.getTagId().toString())
                            .name(tag.getName())
                            .color(tag.getColor())
                            .build();
                }).collect(Collectors.toList()))
                .examples(examples.stream().map(ex -> ProblemExampleDto.builder()
                        .orderNo(ex.getOrderNo())
                        .input(ex.getInput())
                        .expectedOutput(ex.getExpectedOutput())
                        .explanation(ex.getExplanation())
                        .build()).collect(Collectors.toList()))
                .testArchiveObjectKey(testSuite != null ? testSuite.getObjectKey() : null)
                .build();
    }
}
