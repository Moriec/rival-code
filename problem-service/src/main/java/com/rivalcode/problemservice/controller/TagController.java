package com.rivalcode.problemservice.controller;

import com.rivalcode.problemservice.api.TagApi;
import com.rivalcode.problemservice.service.TagService;
import com.rivalcode.contracts.problems.model.TagDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TagController implements TagApi {

    private final TagService tagService;

    @Override
    public List<TagDto> getAllTags() {
        return tagService.getAllTags();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TagDto createTag(TagDto dto) {
        return tagService.createTag(dto);
    }
}