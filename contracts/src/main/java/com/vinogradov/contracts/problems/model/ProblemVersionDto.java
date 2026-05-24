package com.vinogradov.contracts.problems.model;

import com.vinogradov.contracts.problems.enums.CheckerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemVersionDto {
    private String problemVersionId;
    private String problemId;
    private Integer versionNumber;
    private String statementObjectKey;
    private String testsManifestObjectKey;
    private CheckerType checkerType;
    private ProblemLimitsDto limits;
    private Boolean active;
    private Instant createdAt;
}
