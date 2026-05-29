package com.rivalcode.problemservice.service;

import com.rivalcode.problemservice.model.*;
import com.rivalcode.problemservice.repository.*;
import com.rivalcode.contracts.problems.enums.CheckerType;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.contracts.problems.enums.ProblemStatus;
import com.rivalcode.contracts.problems.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemVersionRepository problemVersionRepository;
    private final ProblemExampleRepository problemExampleRepository;
    private final TagRepository tagRepository;
    private final ProblemTagRepository problemTagRepository;
    private final TestSuiteRepository testSuiteRepository;

    public Page<ProblemSummaryDto> listProblems(ProblemFilterRequest filter) {
        PageRequest pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 20,
                Sort.by(Sort.Direction.fromString(
                                filter.getSortDirection() != null ? filter.getSortDirection() : "DESC"),
                        filter.getSortBy() != null ? filter.getSortBy() : "publishedAt"));

        String difficulty = null;
        if (filter.getDifficulties() != null && !filter.getDifficulties().isEmpty()) {
            difficulty = filter.getDifficulties().get(0).name();
        }
        String status = "PUBLISHED";

        Page<Problem> page;
        if (filter.getTagIds() != null && !filter.getTagIds().isEmpty()) {
            List<UUID> tagIds = filter.getTagIds().stream()
                    .map(UUID::fromString).collect(Collectors.toList());
            page = problemRepository.findByTagsAndFilters(tagIds, status, difficulty, pageable);
        } else {
            page = problemRepository.findByFilters(status, difficulty, filter.getSearch(), pageable);
        }

        // ЗАГОТОВКА: фильтрация по solvedByUser и userId
        // if (Boolean.TRUE.equals(filter.getSolvedByUser()) && filter.getUserId() != null) {
        //     Set<UUID> solvedProblemIds = submissionClient.getSolvedProblemIds(filter.getUserId());
        //     // либо исключить решённые, либо оставить только решённые в зависимости от значения solvedByUser
        //     page = page.filter(p -> /* условие на основе solvedProblemIds */);
        // }


        return page.map(this::toSummaryDto);
    }

    public ProblemDetailsDto getProblemDetails(UUID problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));

        ProblemVersion version = problemVersionRepository.findByProblemIdAndActiveTrue(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active version for problem"));

        List<ProblemExample> examples = problemExampleRepository.findByProblemVersionIdOrderByOrderNo(version.getProblemVersionId());
        List<ProblemTag> problemTags = problemTagRepository.findByProblem_ProblemId(problemId);
        List<TagDto> tagDtos = problemTags.stream()
                .map(pt -> {
                    Tag tag = pt.getTag();
                    return TagDto.builder()
                            .tagId(tag.getTagId().toString())
                            .name(tag.getName())
                            .color(tag.getColor())
                            .build();
                })
                .collect(Collectors.toList());

        return ProblemDetailsDto.builder()
                .problemId(problem.getProblemId().toString())
                .problemVersionId(version.getProblemVersionId().toString())
                .slug(problem.getSlug())
                .title(problem.getTitle())
                .statement(version.getStatement())
                .inputSpec(version.getInputSpec())
                .outputSpec(version.getOutputSpec())
                .difficulty(ProblemDifficulty.valueOf(problem.getDifficulty()))
                .status(ProblemStatus.valueOf(problem.getStatus()))
                .checkerType(CheckerType.valueOf(version.getCheckerType()))
                .limits(ProblemLimitsDto.builder()
                        .timeLimitMs(version.getTimeLimitMs())
                        .memoryLimitKb(version.getMemoryLimitKb())
                        .outputLimitBytes(version.getOutputLimitBytes())
                        .build())
                .tags(tagDtos)
                .examples(examples.stream().map(ex -> ProblemExampleDto.builder()
                        .orderNo(ex.getOrderNo())
                        .input(ex.getInput())
                        .expectedOutput(ex.getExpectedOutput())
                        .explanation(ex.getExplanation())
                        .build()).collect(Collectors.toList()))
                .publishedAt(problem.getPublishedAt())
                .updatedAt(problem.getUpdatedAt())
                .build();
    }

    public ProblemExecutionContextDto getExecutionContext(UUID problemId, UUID problemVersionId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));

        ProblemVersion version;
        if (problemVersionId != null) {
            version = problemVersionRepository.findById(problemVersionId)
                    .filter(candidate -> problemId.equals(candidate.getProblemId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem version not found"));
        } else {
            version = problemVersionRepository.findByProblemIdAndActiveTrue(problem.getProblemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active version for problem"));
        }

        TestSuite testSuite = testSuiteRepository.findByProblemVersionId(version.getProblemVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No test suite for problem version"));

        return ProblemExecutionContextDto.builder()
                .problemId(problem.getProblemId().toString())
                .problemVersionId(version.getProblemVersionId().toString())
                .checkerType(CheckerType.valueOf(version.getCheckerType()))
                .limits(ProblemLimitsDto.builder()
                        .timeLimitMs(version.getTimeLimitMs())
                        .memoryLimitKb(version.getMemoryLimitKb())
                        .outputLimitBytes(version.getOutputLimitBytes())
                        .build())
                .testArchiveObjectKey(testSuite.getObjectKey())
                .visibleSampleTestsCount(testSuite.getVisibleSampleTestsCount())
                .customCheckerObjectKey(version.getCustomCheckerObjectKey())
                .build();
    }

    @Transactional
    public ProblemDetailsDto createProblem(CreateProblemRequest request, UUID authorUserId) {
        if (problemRepository.findBySlugIgnoreCase(request.getSlug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug already exists");
        }

        Problem problem = Problem.builder()
                .slug(request.getSlug())
                .title(request.getTitle())
                .difficulty(request.getDifficulty().name())
                .status(request.getStatus() != null ? request.getStatus().name() : ProblemStatus.DRAFT.name())
                .authorUserId(authorUserId)
                .publishedAt(request.getStatus() == ProblemStatus.PUBLISHED ? Instant.now() : null)
                .build();
        problemRepository.save(problem);

        ProblemVersion version = ProblemVersion.builder()
                .problemId(problem.getProblemId())
                .versionNumber(1)
                .statement(request.getStatement())
                .inputSpec(request.getInputSpec())
                .outputSpec(request.getOutputSpec())
                .checkerType(request.getCheckerType().name())
                .timeLimitMs(request.getLimits() != null ? request.getLimits().getTimeLimitMs() : 2000L)
                .memoryLimitKb(request.getLimits() != null ? request.getLimits().getMemoryLimitKb() : 65536L)
                .outputLimitBytes(request.getLimits() != null ? request.getLimits().getOutputLimitBytes() : 65536L)
                .active(true)
                .build();
        problemVersionRepository.save(version);

        if (request.getExamples() != null) {
            int order = 1;
            for (ProblemExampleDto ex : request.getExamples()) {
                ProblemExample example = ProblemExample.builder()
                        .problemVersionId(version.getProblemVersionId())
                        .orderNo(order++)
                        .input(ex.getInput())
                        .expectedOutput(ex.getExpectedOutput())
                        .explanation(ex.getExplanation())
                        .build();
                problemExampleRepository.save(example);
            }
        }

        if (request.getTagIds() != null) {
            for (String tagId : request.getTagIds()) {
                UUID id = UUID.fromString(tagId);
                tagRepository.findById(id).ifPresent(tag -> {
                    ProblemTag pt = ProblemTag.builder()
                            .id(new ProblemTag.ProblemTagId(problem.getProblemId(), tag.getTagId()))
                            .problem(problem)
                            .tag(tag)
                            .build();
                    problemTagRepository.save(pt);
                });
            }
        }

        return getProblemDetails(problem.getProblemId());
    }

    @Transactional
    public ProblemDetailsDto updateProblem(UUID problemId, UpdateProblemRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));

        if (request.getSlug() != null) {
            if (!request.getSlug().equals(problem.getSlug()) &&
                    problemRepository.findBySlugIgnoreCase(request.getSlug()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug already exists");
            }
            problem.setSlug(request.getSlug());
        }
        if (request.getTitle() != null) problem.setTitle(request.getTitle());
        if (request.getDifficulty() != null) problem.setDifficulty(request.getDifficulty().name());
        if (request.getStatus() != null) {
            problem.setStatus(request.getStatus().name());
            if (request.getStatus() == ProblemStatus.PUBLISHED && problem.getPublishedAt() == null) {
                problem.setPublishedAt(Instant.now());
            }
        }

        ProblemVersion currentVersion = problemVersionRepository.findByProblemIdAndActiveTrue(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active version"));

        boolean newVersionRequired = false;
        if (request.getStatement() != null && !request.getStatement().equals(currentVersion.getStatement())) newVersionRequired = true;
        if (request.getInputSpec() != null && !request.getInputSpec().equals(currentVersion.getInputSpec())) newVersionRequired = true;
        if (request.getOutputSpec() != null && !request.getOutputSpec().equals(currentVersion.getOutputSpec())) newVersionRequired = true;
        if (request.getCheckerType() != null && !request.getCheckerType().name().equals(currentVersion.getCheckerType())) newVersionRequired = true;
        if (request.getLimits() != null) {
            if (request.getLimits().getTimeLimitMs() != null && !request.getLimits().getTimeLimitMs().equals(currentVersion.getTimeLimitMs())) newVersionRequired = true;
            if (request.getLimits().getMemoryLimitKb() != null && !request.getLimits().getMemoryLimitKb().equals(currentVersion.getMemoryLimitKb())) newVersionRequired = true;
            if (request.getLimits().getOutputLimitBytes() != null && !request.getLimits().getOutputLimitBytes().equals(currentVersion.getOutputLimitBytes())) newVersionRequired = true;
        }

        if (newVersionRequired) {
            currentVersion.setActive(false);
            problemVersionRepository.save(currentVersion);

            ProblemVersion lastVersion = problemVersionRepository.findTopByProblemIdOrderByVersionNumberDesc(problemId).orElse(currentVersion);
            ProblemVersion newVersion = ProblemVersion.builder()
                    .problemId(problemId)
                    .versionNumber(lastVersion.getVersionNumber() + 1)
                    .statement(request.getStatement() != null ? request.getStatement() : currentVersion.getStatement())
                    .inputSpec(request.getInputSpec() != null ? request.getInputSpec() : currentVersion.getInputSpec())
                    .outputSpec(request.getOutputSpec() != null ? request.getOutputSpec() : currentVersion.getOutputSpec())
                    .checkerType(request.getCheckerType() != null ? request.getCheckerType().name() : currentVersion.getCheckerType())
                    .timeLimitMs(request.getLimits() != null && request.getLimits().getTimeLimitMs() != null ? request.getLimits().getTimeLimitMs() : currentVersion.getTimeLimitMs())
                    .memoryLimitKb(request.getLimits() != null && request.getLimits().getMemoryLimitKb() != null ? request.getLimits().getMemoryLimitKb() : currentVersion.getMemoryLimitKb())
                    .outputLimitBytes(request.getLimits() != null && request.getLimits().getOutputLimitBytes() != null ? request.getLimits().getOutputLimitBytes() : currentVersion.getOutputLimitBytes())
                    .active(true)
                    .build();
            problemVersionRepository.save(newVersion);

            List<ProblemExample> oldExamples = problemExampleRepository.findByProblemVersionIdOrderByOrderNo(currentVersion.getProblemVersionId());
            for (ProblemExample old : oldExamples) {
                ProblemExample newEx = ProblemExample.builder()
                        .problemVersionId(newVersion.getProblemVersionId())
                        .orderNo(old.getOrderNo())
                        .input(old.getInput())
                        .expectedOutput(old.getExpectedOutput())
                        .explanation(old.getExplanation())
                        .build();
                problemExampleRepository.save(newEx);
            }
        } else {
            if (request.getStatement() != null) currentVersion.setStatement(request.getStatement());
            if (request.getInputSpec() != null) currentVersion.setInputSpec(request.getInputSpec());
            if (request.getOutputSpec() != null) currentVersion.setOutputSpec(request.getOutputSpec());
            if (request.getCheckerType() != null) currentVersion.setCheckerType(request.getCheckerType().name());
            if (request.getLimits() != null) {
                if (request.getLimits().getTimeLimitMs() != null) currentVersion.setTimeLimitMs(request.getLimits().getTimeLimitMs());
                if (request.getLimits().getMemoryLimitKb() != null) currentVersion.setMemoryLimitKb(request.getLimits().getMemoryLimitKb());
                if (request.getLimits().getOutputLimitBytes() != null) currentVersion.setOutputLimitBytes(request.getLimits().getOutputLimitBytes());
            }
            problemVersionRepository.save(currentVersion);
        }

        if (request.getTagIds() != null) {
            problemTagRepository.deleteByProblem_ProblemId(problemId);
            for (String tagId : request.getTagIds()) {
                UUID id = UUID.fromString(tagId);
                tagRepository.findById(id).ifPresent(tag -> {
                    ProblemTag pt = ProblemTag.builder()
                            .id(new ProblemTag.ProblemTagId(problem.getProblemId(), tag.getTagId()))
                            .problem(problem)
                            .tag(tag)
                            .build();
                    problemTagRepository.save(pt);
                });
            }
        }

        problemRepository.save(problem);
        return getProblemDetails(problemId);
    }


    private ProblemSummaryDto toSummaryDto(Problem problem) {
        List<ProblemTag> problemTags = problemTagRepository.findByProblem_ProblemId(problem.getProblemId());
        List<TagDto> tags = problemTags.stream()
                .map(pt -> {
                    Tag tag = pt.getTag();
                    return TagDto.builder()
                            .tagId(tag.getTagId().toString())
                            .name(tag.getName())
                            .color(tag.getColor())
                            .build();
                })
                .collect(Collectors.toList());

        return ProblemSummaryDto.builder()
                .problemId(problem.getProblemId().toString())
                .slug(problem.getSlug())
                .title(problem.getTitle())
                .difficulty(ProblemDifficulty.valueOf(problem.getDifficulty()))
                .status(ProblemStatus.valueOf(problem.getStatus()))
                .tags(tags)
                .acceptedCount(problem.getAcceptedCount())
                .attemptsCount(problem.getAttemptsCount())
                .publishedAt(problem.getPublishedAt())
                .build();
    }
}
