package com.rivalcode.problemservice.api;

import com.rivalcode.contracts.problems.model.TagDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/problems/tags")
@Tag(name = "Теги", description = "Управление тегами задач. Для создания и удаления требуется роль ADMIN.")
public interface TagApi {

    @Operation(
            summary = "Получить список всех тегов",
            description = "Возвращает все доступные теги. Можно использовать для фильтрации задач.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список тегов",
                    content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping
    List<TagDto> getAllTags();

    @Operation(
            summary = "Создать новый тег (только ADMIN)",
            description = "Создаёт тег с указанным названием. Название должно быть уникальным.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Тег создан",
                    content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "400", description = "Тег уже существует",
                    content = @Content(examples = @ExampleObject(value = "{\"error\": \"Tag already exists\"}"))),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав ADMIN")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    TagDto createTag(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные нового тега",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TagDto.class),
                            examples = @ExampleObject(
                                    name = "Пример тега",
                                    value = """
                                    {
                                        "name": "dynamic programming",
                                        "color": "#FF5733"
                                    }
                                    """)))
            @Valid @RequestBody TagDto dto
    );

}