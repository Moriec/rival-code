package com.rivalcode.submissionservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_judge_results", schema = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedJudgeResultEntity {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "result_event_hash", nullable = false, length = 128)
    private String resultEventHash;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
