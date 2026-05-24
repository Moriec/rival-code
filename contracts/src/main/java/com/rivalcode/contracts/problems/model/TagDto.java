package com.rivalcode.contracts.problems.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDto {
    private String tagId;
    private String name;
    private String color;
}
