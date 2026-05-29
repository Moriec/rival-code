package com.rivalcode.submissionservice.model;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import com.rivalcode.contracts.submissions.enums.SubmissionMode;
import com.rivalcode.contracts.submissions.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions", schema = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEntity {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "problem_version_id", nullable = false)
    private UUID problemVersionId;

    @Column(name = "duel_id")
    private UUID duelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProgrammingLanguages language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", length = 64)
    private JudgeStatus overallStatus;

    private Boolean accepted;

    @Column(name = "max_time_ms")
    private Long maxTimeMs;

    @Column(name = "max_memory_kb")
    private Long maxMemoryKb;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "judged_at")
    private Instant judgedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
