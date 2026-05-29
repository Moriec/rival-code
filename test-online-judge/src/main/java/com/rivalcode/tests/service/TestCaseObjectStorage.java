package com.rivalcode.tests.service;

import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.contracts.submissions.model.TestCaseFileRef;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TestCaseObjectStorage {

    private final MinioClient minioClient;

    @Value("${app.minio.test-cases-bucket:online-judge-test-cases}")
    private String bucket;

    public TestCase createTestCase(String submissionId, int index, String input, String expectedOutput) {
        String inputObjectKey = "test-online-judge/" + submissionId + "/" + index + ".in";
        String expectedObjectKey = "test-online-judge/" + submissionId + "/" + index + ".out";

        putUtf8Object(inputObjectKey, input);
        putUtf8Object(expectedObjectKey, expectedOutput);

        return TestCase.builder()
                .inputFile(TestCaseFileRef.builder()
                        .bucket(bucket)
                        .objectKey(inputObjectKey)
                        .build())
                .expectedOutputFile(TestCaseFileRef.builder()
                        .bucket(bucket)
                        .objectKey(expectedObjectKey)
                        .build())
                .build();
    }

    private void putUtf8Object(String objectKey, String content) {
        try {
            ensureBucketExists();
            byte[] bytes = (content != null ? content : "").getBytes(StandardCharsets.UTF_8);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("text/plain; charset=utf-8")
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload test case object " + bucket + "/" + objectKey, e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
