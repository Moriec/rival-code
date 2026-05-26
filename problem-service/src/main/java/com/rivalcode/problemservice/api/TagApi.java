package com.rivalcode.problemservice.api;

import com.rivalcode.contracts.problems.model.TagDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/problems/tags")
@Tag(name = "Теги", description = "Управление тегами задач")
public interface TagApi {

    @GetMapping
    @Operation(summary = "Список всех тегов")
    List<TagDto> getAllTags();

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать тег")
    TagDto createTag(@RequestBody TagDto dto);
}