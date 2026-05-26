package com.rivalcode.problemservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.assets-bucket}")
    private String assetsBucket;

    @Value("${minio.test-archives-bucket}")
    private String testArchivesBucket;

    public String uploadFileToAssets(MultipartFile file, UUID problemId) {
        String objectKey = "problems/" + problemId + "/assets/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        putObject(assetsBucket, objectKey, file);
        return objectKey;
    }

    public String uploadTestArchive(MultipartFile file, UUID problemId, UUID versionId) {
        String objectKey = "problems/" + problemId + "/versions/" + versionId + "/tests/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        putObject(testArchivesBucket, objectKey, file);
        return objectKey;
    }

    public String getPresignedUrl(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(3600)
                            .build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to generate presigned URL", e);
        }
    }

    public String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    private void putObject(String bucket, String objectKey, MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload file to MinIO", e);
        }
    }
}