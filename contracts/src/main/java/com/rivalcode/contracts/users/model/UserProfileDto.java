package com.rivalcode.contracts.users.model;

import com.rivalcode.contracts.users.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String userId;
    private String email;
    private String username;
    private String displayName;
    private AvatarDto avatar;
    private List<UserRole> roles;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
