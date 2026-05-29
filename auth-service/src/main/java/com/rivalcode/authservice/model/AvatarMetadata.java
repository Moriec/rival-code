package com.rivalcode.authservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "avatar_metadata", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarMetadata {

    @Id
    @Column(name = "avatar_id")
    private UUID avatarId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private Boolean active;

    @PrePersist
    void prePersist() {
        if (avatarId == null) avatarId = UUID.randomUUID();
        if (uploadedAt == null) uploadedAt = Instant.now();
        if (active == null) active = false;
    }
}