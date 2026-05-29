package com.rivalcode.submissionservice.api;

import com.rivalcode.contracts.submissions.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api")
@Tag(name = "Submissions", description = "Submission lifecycle and public verdicts")
public interface SubmissionApi {

    @PostMapping("/submissions")
    @Operation(summary = "Create practice or duel submission")
    SubmissionCreatedResponse createSubmission(@Valid @RequestBody CreateSubmissionRequest request);

    @GetMapping("/submissions/{submissionId}")
    @Operation(summary = "Get submission summary")
    SubmissionSummaryDto getSubmission(@PathVariable UUID submissionId);

    @GetMapping("/submissions/{submissionId}/verdict")
    @Operation(summary = "Get public verdict without hidden test leakage")
    PublicSubmissionVerdict getVerdict(@PathVariable UUID submissionId);

    @GetMapping("/users/{userId}/submissions")
    @Operation(summary = "Get user submission history")
    List<SubmissionSummaryDto> getUserSubmissions(@PathVariable UUID userId);
}
