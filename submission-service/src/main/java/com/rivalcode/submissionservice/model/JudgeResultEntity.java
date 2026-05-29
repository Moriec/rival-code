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
@Table(name = "judge_results", schema = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultEntity {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 64)
    private JudgeStatus overallStatus;

    @Column(name = "max_time_ms")
    private Long maxTimeMs;

    @Column(name = "max_memory_kb")
    private Long maxMemoryKb;

    @Column(name = "compilation_error", columnDefinition = "TEXT")
    private String compilationError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_result_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode rawResultJson;

    @Column(name = "judge_log_object_key", columnDefinition = "TEXT")
    private String judgeLogObjectKey;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
