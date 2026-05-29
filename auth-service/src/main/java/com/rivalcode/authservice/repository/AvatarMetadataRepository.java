package com.rivalcode.authservice.repository;

import com.rivalcode.authservice.model.AvatarMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvatarMetadataRepository extends JpaRepository<AvatarMetadata, UUID> {
    List<AvatarMetadata> findByUserIdOrderByUploadedAtDesc(UUID userId);
    Optional<AvatarMetadata> findByUserIdAndActiveTrue(UUID userId);
}