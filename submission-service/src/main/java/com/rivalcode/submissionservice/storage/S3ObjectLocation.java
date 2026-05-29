package com.rivalcode.submissionservice.storage;

import org.springframework.util.StringUtils;

import java.net.URI;

public record S3ObjectLocation(String bucket, String objectKey) {

    public static S3ObjectLocation parse(String value, String defaultBucket) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("S3 object location is required");
        }

        if (value.startsWith("s3://")) {
            URI uri = URI.create(value);
            String bucket = uri.getHost();
            String objectKey = uri.getPath() != null ? uri.getPath().replaceFirst("^/", "") : "";
            if (!StringUtils.hasText(bucket)) {
                throw new IllegalArgumentException("S3 bucket is required in " + value);
            }
            return new S3ObjectLocation(bucket, objectKey);
        }

        if (!StringUtils.hasText(defaultBucket)) {
            throw new IllegalArgumentException("Default S3 bucket is required for object key " + value);
        }
        return new S3ObjectLocation(defaultBucket, value);
    }

    public S3ObjectLocation asPrefix() {
        if (!StringUtils.hasText(objectKey) || objectKey.endsWith("/")) {
            return this;
        }
        return new S3ObjectLocation(bucket, objectKey + "/");
    }
}
