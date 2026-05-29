package com.rivalcode.contracts.problems.model;

import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.contracts.problems.enums.ProblemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemFilterRequest {
    private String search;
    private List<ProblemDifficulty> difficulties;
    private List<ProblemStatus> statuses;
    private List<String> tagIds;
    private Boolean solvedByUser;
    private String userId;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
