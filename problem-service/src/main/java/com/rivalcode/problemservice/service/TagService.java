package com.rivalcode.problemservice.service;

import com.rivalcode.problemservice.model.Tag;
import com.rivalcode.problemservice.repository.TagRepository;
import com.rivalcode.contracts.problems.model.TagDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<TagDto> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagDto createTag(TagDto dto) {
        if (tagRepository.findByNameIgnoreCase(dto.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag already exists");
        }
        Tag tag = Tag.builder()
                .name(dto.getName())
                .color(dto.getColor())
                .build();
        tag = tagRepository.save(tag);
        return toDto(tag);
    }

    private TagDto toDto(Tag tag) {
        return TagDto.builder()
                .tagId(tag.getTagId().toString())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }
}