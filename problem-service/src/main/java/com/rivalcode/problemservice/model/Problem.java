package com.rivalcode.problemservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "problems", schema = "problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    @Column(name = "problem_id")
    private UUID problemId;

    @Column(nullable = false, length = 128)
    private String slug;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(nullable = false, length = 64)
    private String difficulty;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "accepted_count")
    private Long acceptedCount;

    @Column(name = "attempts_count")
    private Long attemptsCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (problemId == null) problemId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (acceptedCount == null) acceptedCount = 0L;
        if (attemptsCount == null) attemptsCount = 0L;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}