package com.rivalcode.authservice.mapper;

import com.rivalcode.authservice.model.User;
import com.rivalcode.contracts.users.model.UserProfileDto;
import com.rivalcode.contracts.users.enums.UserRole;

import java.util.List;

public class UserMapper {

    public static UserProfileDto toDto(User user, List<UserRole> roles, String avatarUrl) {
        return UserProfileDto.builder()
                .userId(user.getUserId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .roles(roles)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}