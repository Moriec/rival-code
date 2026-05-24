package com.rivalcode.contracts.users.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarDto {
    private String avatarId;
    private String objectKey;
    private String url;
    private String contentType;
    private Long sizeBytes;
    private Instant uploadedAt;
}
