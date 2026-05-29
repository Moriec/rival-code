package com.rivalcode.submissionservice.storage;

import com.rivalcode.contracts.submissions.model.TestCase;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioTestSuiteResolver {

    private final MinioClient minioClient;
    private final TestSuiteObjectMapper testSuiteObjectMapper;

    @Value("${app.minio.default-bucket:olimp-tests}")
    private String defaultBucket;

    public ResolvedTestSuite resolve(String testArchiveObjectKey) {
        S3ObjectLocation location = S3ObjectLocation.parse(testArchiveObjectKey, defaultBucket).asPrefix();
        List<String> objectNames = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(location.bucket())
                    .prefix(location.objectKey())
                    .recursive(true)
                    .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    continue;
                }
                objectNames.add(item.objectName());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list test suite " + testArchiveObjectKey, e);
        }

        List<TestCase> testCases = testSuiteObjectMapper.toTestCases(location.bucket(), objectNames);

        return new ResolvedTestSuite(
                location.bucket(),
                location.objectKey(),
                testCases
        );
    }

    public String readUtf8(String objectKey) {
        S3ObjectLocation location = S3ObjectLocation.parse(objectKey, defaultBucket);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(location.bucket())
                .object(location.objectKey())
                .build())) {
            return readAll(inputStream);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read object " + objectKey, e);
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
