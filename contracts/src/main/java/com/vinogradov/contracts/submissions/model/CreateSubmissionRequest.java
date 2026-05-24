package com.vinogradov.contracts.submissions.model;

import com.vinogradov.contracts.submissions.enums.SubmissionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubmissionRequest {
    private String userId;
    private String problemId;
    private String problemVersionId;
    private String duelId;
    private SubmissionMode mode;
    private UserCode userCode;
}
