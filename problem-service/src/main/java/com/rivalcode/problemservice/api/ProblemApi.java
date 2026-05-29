package com.rivalcode.problemservice.api;

import com.rivalcode.contracts.problems.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/problems")
@Tag(name = "Задачи", description = "Просмотр и управление задачами. Для создания/обновления требуется роль ADMIN.")
public interface ProblemApi {

    @GetMapping
    @Operation(summary = "Список задач с фильтрацией и пагинацией",
            security = @SecurityRequirement(name = "bearerAuth"))
    Page<ProblemSummaryDto> listProblems(
            @ParameterObject
            @Parameter(description = "Фильтры и пагинация")
            @Valid ProblemFilterRequest filter
    );

    @GetMapping("/{problemId}")
    @Operation(summary = "Детальная карточка задачи",
            security = @SecurityRequirement(name = "bearerAuth"))
    ProblemDetailsDto getProblemDetails(
            @Parameter(description = "UUID задачи", example = "4fff52f4-4a6e-4d69-9260-e4dc898b674b")
            @PathVariable UUID problemId
    );

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать задачу (только ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth"))
    ProblemDetailsDto createProblem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Пример задачи",
                                    value = """
                        {
                            "slug": "two-sum",
                            "title": "Two Sum",
                            "statement": "Найдите два числа, сумма которых равна заданному.",
                            "difficulty": "EASY",
                            "status": "PUBLISHED",
                            "checkerType": "STANDARD",
                            "limits": {
                                "timeLimitMs": 1000,
                                "memoryLimitKb": 65536,
                                "outputLimitBytes": 65536
                            },
                            "tagIds": [
                                "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                "4fff52f4-4a6e-4d69-9260-e4dc898b674b"
                            ],
                            "examples": [
                                {
                                    "orderNo": 1,
                                    "input": "2 7 11 15",
                                    "expectedOutput": "0 1"
                                }
                            ]
                        }
                        """)))
            @Valid @RequestBody CreateProblemRequest request
    );

    @PatchMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить задачу (только ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth"))
    ProblemDetailsDto updateProblem(
            @Parameter(description = "UUID задачи", example = "4fff52f4-4a6e-4d69-9260-e4dc898b674b")
            @PathVariable UUID problemId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Пример обновления",
                                    value = """
                        {
                            "title": "Новое название",
                            "statement": "Новое условие",
                            "difficulty": "MEDIUM",
                            "tagIds": [
                                "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                            ]
                        }
                        """)))
            @Valid @RequestBody UpdateProblemRequest request
    );
}