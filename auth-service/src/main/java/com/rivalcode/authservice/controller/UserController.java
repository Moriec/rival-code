package com.rivalcode.authservice.controller;

import com.rivalcode.authservice.api.UserApi;
import com.rivalcode.authservice.exception.UnauthorizedException;
import com.rivalcode.authservice.model.AvatarContent;
import com.rivalcode.authservice.model.AvatarMetadata;
import com.rivalcode.authservice.service.AvatarService;
import com.rivalcode.authservice.service.UserService;
import com.rivalcode.contracts.users.model.UpdateProfileRequest;
import com.rivalcode.contracts.users.model.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final AvatarService avatarService;

    @Override
    public UserProfileDto getMyProfile() {
        UUID userId = getCurrentUserId();
        return userService.getProfile(userId);
    }

    @Override
    public UserProfileDto getUserProfile(UUID userId) {
        return userService.getProfile(userId);
    }

    @Override
    public UserProfileDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getCurrentUserId();
        return userService.updateProfile(userId, request);
    }

    @Override
    public AvatarMetadata uploadAvatar(@RequestParam("file") MultipartFile file) {
        UUID userId = getCurrentUserId();
        return avatarService.uploadAvatar(userId, file);
    }

    @Override
    public ResponseEntity<Resource> getAvatarFile(UUID avatarId) {
        AvatarContent content = avatarService.getAvatarContent(avatarId);
        MediaType mediaType = parseMediaType(content.metadata().getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.metadata().getSizeBytes())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(new InputStreamResource(content.stream()));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return UUID.fromString((String) auth.getPrincipal());
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
