package com.rivalcode.problemservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "problem_versions", schema = "problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemVersion {

    @Id
    @Column(name = "problem_version_id")
    private UUID problemVersionId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "input_spec", columnDefinition = "TEXT")
    private String inputSpec;

    @Column(name = "output_spec", columnDefinition = "TEXT")
    private String outputSpec;

    @Column(name = "statement_object_key", columnDefinition = "TEXT")
    private String statementObjectKey;

    @Column(name = "tests_manifest_object_key", columnDefinition = "TEXT")
    private String testsManifestObjectKey;

    @Column(name = "checker_type", nullable = false, length = 64)
    private String checkerType;

    @Column(name = "custom_checker_object_key", columnDefinition = "TEXT")
    private String customCheckerObjectKey;

    @Column(name = "time_limit_ms", nullable = false)
    private Long timeLimitMs;

    @Column(name = "memory_limit_kb", nullable = false)
    private Long memoryLimitKb;

    @Column(name = "output_limit_bytes", nullable = false)
    private Long outputLimitBytes;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (problemVersionId == null) problemVersionId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}