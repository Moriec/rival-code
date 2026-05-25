package com.rivalcode.authservice.service;

import com.rivalcode.authservice.exception.EntityNotFoundException;
import com.rivalcode.authservice.exception.ForbiddenException;
import com.rivalcode.authservice.model.AvatarMetadata;
import com.rivalcode.authservice.model.User;
import com.rivalcode.authservice.repository.AvatarMetadataRepository;
import com.rivalcode.authservice.repository.UserRepository;
import com.rivalcode.contracts.users.model.AvatarDto;
import com.rivalcode.contracts.users.model.UserProfileDto;
import com.rivalcode.contracts.users.model.UpdateProfileRequest;
import com.rivalcode.contracts.users.enums.UserRole;
import com.rivalcode.authservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AvatarMetadataRepository avatarMetadataRepository;
    private final UserRoleRepository userRoleRepository;
    private final MinioService minioService;

    public UserProfileDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        AvatarMetadata activeAvatar = avatarMetadataRepository.findByUserIdAndActiveTrue(userId).orElse(null);
        AvatarDto avatarDto = null;
        if (activeAvatar != null) {
            String url = activeAvatar.getUrl();
            if (url == null || url.isEmpty()) {
                try {
                    url = minioService.getPresignedUrl(activeAvatar.getObjectKey());
                } catch (Exception e) {
                    log.warn("Failed to generate presigned URL for avatar {} of user {}",
                            activeAvatar.getAvatarId(), userId, e);
                    url = null;
                }
            }
            avatarDto = AvatarDto.builder()
                    .avatarId(activeAvatar.getAvatarId().toString())
                    .objectKey(activeAvatar.getObjectKey())
                    .url(url)
                    .contentType(activeAvatar.getContentType())
                    .sizeBytes(activeAvatar.getSizeBytes())
                    .uploadedAt(activeAvatar.getUploadedAt())
                    .build();
        }

        List<UserRole> roles = userRoleRepository.findByUser_UserId(userId)
                .stream()
                .map(r -> UserRole.valueOf(r.getId().getRole()))
                .collect(Collectors.toList());

        return UserProfileDto.builder()
                .userId(user.getUserId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatar(avatarDto)
                .roles(roles)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getAvatarId() != null) {
            UUID avatarId = UUID.fromString(request.getAvatarId());
            AvatarMetadata newAvatar = avatarMetadataRepository.findById(avatarId)
                    .orElseThrow(() -> new EntityNotFoundException("Avatar not found"));
            if (!newAvatar.getUserId().equals(userId)) {
                throw new ForbiddenException("Avatar does not belong to user");
            }
            avatarMetadataRepository.findByUserIdAndActiveTrue(userId).ifPresent(active -> {
                active.setActive(false);
                avatarMetadataRepository.save(active);
            });
            newAvatar.setActive(true);
            avatarMetadataRepository.save(newAvatar);
        }

        userRepository.save(user);
        return getProfile(userId);
    }
}