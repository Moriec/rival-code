package com.rivalcode.submissionservice.model;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "test_case_results", schema = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResultEntity {

    @Id
    @Column(name = "test_case_result_id")
    private UUID testCaseResultId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private JudgeStatus status;

    @Column(name = "time_ms")
    private Long timeMs;

    @Column(name = "memory_kb")
    private Long memoryKb;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Boolean visible;
}
