package com.rivalcode.contracts.submissions.model;

import com.rivalcode.contracts.submissions.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionCreatedResponse {
    private String submissionId;
    private SubmissionStatus status;
    private Instant createdAt;
}
