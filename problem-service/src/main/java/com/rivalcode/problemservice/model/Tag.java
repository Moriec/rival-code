package com.rivalcode.problemservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tags", schema = "problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @Column(name = "tag_id")
    private UUID tagId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String color;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (tagId == null) tagId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}