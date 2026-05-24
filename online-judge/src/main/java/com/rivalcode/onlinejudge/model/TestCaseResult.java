package com.rivalcode.onlinejudge.model;

import com.rivalcode.onlinejudge.enums.JudgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private JudgeStatus status;
    private Long timeMs;
    private Long memoryKb;
    private String input;
    private String actualOutput;
    private String expectedOutput;
    private String message;
}
