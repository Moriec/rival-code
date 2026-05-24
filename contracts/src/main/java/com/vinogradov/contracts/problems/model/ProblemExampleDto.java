package com.vinogradov.contracts.problems.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemExampleDto {
    private Integer orderNo;
    private String input;
    private String expectedOutput;
    private String explanation;
}
