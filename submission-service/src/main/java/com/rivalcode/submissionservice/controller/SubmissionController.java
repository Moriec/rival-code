package com.rivalcode.submissionservice.controller;

import com.rivalcode.contracts.submissions.model.*;
import com.rivalcode.submissionservice.api.SubmissionApi;
import com.rivalcode.submissionservice.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SubmissionController implements SubmissionApi {

    private final SubmissionService submissionService;

    @Override
    public SubmissionCreatedResponse createSubmission(CreateSubmissionRequest request) {
        UUID currentUserId = getCurrentUserId();
        return submissionService.createSubmission(request, currentUserId);
    }

    @Override
    public SubmissionSummaryDto getSubmission(UUID submissionId) {
        return submissionService.getSubmission(submissionId);
    }

    @Override
    public PublicSubmissionVerdict getVerdict(UUID submissionId) {
        return submissionService.getVerdict(submissionId);
    }

    @Override
    public List<SubmissionSummaryDto> getUserSubmissions(UUID userId) {
        return submissionService.getUserSubmissions(userId);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return UUID.fromString(userId);
    }
}
