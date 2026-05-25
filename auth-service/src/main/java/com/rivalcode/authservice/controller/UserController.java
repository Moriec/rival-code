package com.rivalcode.authservice.controller;

import com.rivalcode.authservice.api.UserApi;
import com.rivalcode.authservice.exception.UnauthorizedException;
import com.rivalcode.authservice.model.AvatarMetadata;
import com.rivalcode.authservice.service.AvatarService;
import com.rivalcode.authservice.service.UserService;
import com.rivalcode.contracts.users.model.UpdateProfileRequest;
import com.rivalcode.contracts.users.model.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public UserProfileDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getCurrentUserId();
        return userService.updateProfile(userId, request);
    }

    @Override
    public AvatarMetadata uploadAvatar(@RequestParam("file") MultipartFile file) {
        UUID userId = getCurrentUserId();
        return avatarService.uploadAvatar(userId, file);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return UUID.fromString((String) auth.getPrincipal());
    }
}