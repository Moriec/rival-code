package com.rivalcode.problemservice.api;

import com.rivalcode.contracts.problems.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/problems")
@Tag(name = "Задачи", description = "Архив задач, CRUD и выбор для дуэли")
public interface ProblemApi {

    @GetMapping
    @Operation(summary = "Список задач с фильтрацией")
    Page<ProblemSummaryDto> listProblems(@Parameter(description = "Фильтры и пагинация") ProblemFilterRequest filter);

    @GetMapping("/{problemId}")
    @Operation(summary = "Детальная карточка задачи")
    ProblemDetailsDto getProblemDetails(@PathVariable UUID problemId);

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать новую задачу")
    ProblemDetailsDto createProblem(@Valid @RequestBody CreateProblemRequest request);

    @PatchMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить задачу")
    ProblemDetailsDto updateProblem(@PathVariable UUID problemId, @Valid @RequestBody UpdateProblemRequest request);
}