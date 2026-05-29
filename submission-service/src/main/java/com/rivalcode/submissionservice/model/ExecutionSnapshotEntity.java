package com.rivalcode.submissionservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.rivalcode.contracts.problems.enums.CheckerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_snapshots", schema = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionSnapshotEntity {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "problem_version_id", nullable = false)
    private UUID problemVersionId;

    @Column(name = "time_limit_ms", nullable = false)
    private Long timeLimitMs;

    @Column(name = "memory_limit_kb", nullable = false)
    private Long memoryLimitKb;

    @Column(name = "output_limit_bytes")
    private Long outputLimitBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "checker_type", nullable = false, length = 64)
    private CheckerType checkerType;

    @Column(name = "custom_checker_code", columnDefinition = "TEXT")
    private String customCheckerCode;

    @Column(name = "test_archive_object_key", columnDefinition = "TEXT")
    private String testArchiveObjectKey;

    @Column(name = "visible_sample_tests_count", nullable = false)
    private Integer visibleSampleTestsCount;

    @Column(name = "store_full_judge_log", nullable = false)
    private Boolean storeFullJudgeLog;

    @Column(name = "rated_mode", nullable = false)
    private Boolean ratedMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
