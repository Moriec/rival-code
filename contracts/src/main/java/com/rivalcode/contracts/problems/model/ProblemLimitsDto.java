package com.rivalcode.contracts.problems.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemLimitsDto {
    private Long timeLimitMs;
    private Long memoryLimitKb;
    private Long outputLimitBytes;
}
