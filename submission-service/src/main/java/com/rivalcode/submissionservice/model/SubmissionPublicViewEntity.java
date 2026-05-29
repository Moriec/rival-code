package com.rivalcode.submissionservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submission_public_views", schema = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionPublicViewEntity {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 64)
    private JudgeStatus overallStatus;

    @Column(name = "passed_tests", nullable = false)
    private Integer passedTests;

    @Column(name = "total_tests", nullable = false)
    private Integer totalTests;

    @Column(name = "max_time_ms")
    private Long maxTimeMs;

    @Column(name = "max_memory_kb")
    private Long maxMemoryKb;

    @Column(name = "compilation_error", columnDefinition = "TEXT")
    private String compilationError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visible_tests_json", columnDefinition = "jsonb")
    private JsonNode visibleTestsJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
