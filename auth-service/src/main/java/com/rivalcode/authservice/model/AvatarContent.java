package com.rivalcode.authservice.model;

import io.minio.GetObjectResponse;

public record AvatarContent(AvatarMetadata metadata, GetObjectResponse stream) {
}
