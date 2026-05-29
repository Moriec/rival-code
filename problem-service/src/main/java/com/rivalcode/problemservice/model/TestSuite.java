package com.rivalcode.problemservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_suites", schema = "problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSuite {

    @Id
    @Column(name = "test_suite_id")
    private UUID testSuiteId;

    @Column(name = "problem_version_id", nullable = false)
    private UUID problemVersionId;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Column(name = "visible_sample_tests_count", nullable = false)
    private Integer visibleSampleTestsCount;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (testSuiteId == null) testSuiteId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}