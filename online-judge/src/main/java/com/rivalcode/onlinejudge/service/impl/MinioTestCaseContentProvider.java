package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.contracts.submissions.model.TestCaseFileRef;
import com.rivalcode.onlinejudge.model.ResolvedTestCase;
import com.rivalcode.onlinejudge.service.TestCaseContentProvider;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MinioTestCaseContentProvider implements TestCaseContentProvider {

    private final MinioClient minioClient;

    @Value("${app.minio.test-cases-bucket:online-judge-test-cases}")
    private String defaultBucket;

    @Value("${app.minio.max-test-case-bytes:67108864}")
    private long maxTestCaseBytes;

    @Override
    public ResolvedTestCase resolve(TestCase testCase) {
        String input = readObject(testCase.getInputFile(), "inputFile");
        String expectedOutput = readObject(testCase.getExpectedOutputFile(), "expectedOutputFile");
        return new ResolvedTestCase(input, expectedOutput);
    }

    private String readObject(TestCaseFileRef ref, String fieldName) {
        if (ref == null || ref.getObjectKey() == null || ref.getObjectKey().isBlank()) {
            throw new IllegalArgumentException("testCase." + fieldName + ".objectKey is required");
        }

        String bucket = ref.getBucket() != null && !ref.getBucket().isBlank()
                ? ref.getBucket()
                : defaultBucket;

        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(ref.getObjectKey())
                .build())) {
            return readUtf8(inputStream, fieldName, bucket, ref.getObjectKey());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read test case " + fieldName
                    + " from MinIO object " + bucket + "/" + ref.getObjectKey(), e);
        }
    }

    private String readUtf8(InputStream inputStream, String fieldName, String bucket, String objectKey) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            totalBytes += read;
            if (totalBytes > maxTestCaseBytes) {
                throw new IllegalStateException("testCase." + fieldName + " is too large: "
                        + bucket + "/" + objectKey);
            }
            output.write(buffer, 0, read);
        }

        return output.toString(StandardCharsets.UTF_8);
    }
}
