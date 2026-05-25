package com.rivalcode.authservice.service;

import com.rivalcode.authservice.model.AvatarMetadata;
import com.rivalcode.authservice.repository.AvatarMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private final AvatarMetadataRepository avatarMetadataRepository;
    private final MinioService minioService;

    @Transactional
    public AvatarMetadata uploadAvatar(UUID userId, MultipartFile file) {
        String objectKey = minioService.uploadFile(file, userId);

        AvatarMetadata metadata = AvatarMetadata.builder()
                .userId(userId)
                .objectKey(objectKey)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .active(false)
                .build();
        return avatarMetadataRepository.save(metadata);
    }
}