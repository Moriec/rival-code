package com.rivalcode.submissionservice.controller;

import com.rivalcode.contracts.submissions.model.SubmissionExecutionContext;
import com.rivalcode.submissionservice.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/submissions")
@RequiredArgsConstructor
@Tag(name = "Internal Submissions", description = "Internal submission-service operations for backend services")
public class InternalSubmissionController {

    private final SubmissionService submissionService;

    @GetMapping("/{submissionId}/execution-context")
    @Operation(summary = "Get immutable execution context for a submission")
    public SubmissionExecutionContext getExecutionContext(@PathVariable UUID submissionId) {
        return submissionService.getExecutionContext(submissionId);
    }
}
