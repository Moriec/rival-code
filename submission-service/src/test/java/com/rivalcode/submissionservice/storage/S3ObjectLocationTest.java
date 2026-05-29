package com.rivalcode.submissionservice.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3ObjectLocationTest {

    @Test
    void parsesS3Location() {
        S3ObjectLocation location = S3ObjectLocation.parse("s3://olimp-tests/2063/A/", "fallback");

        assertThat(location.bucket()).isEqualTo("olimp-tests");
        assertThat(location.objectKey()).isEqualTo("2063/A/");
    }

    @Test
    void usesDefaultBucketForPlainObjectKey() {
        S3ObjectLocation location = S3ObjectLocation.parse("2063/A/001.in", "olimp-tests");

        assertThat(location.bucket()).isEqualTo("olimp-tests");
        assertThat(location.objectKey()).isEqualTo("2063/A/001.in");
    }
}
