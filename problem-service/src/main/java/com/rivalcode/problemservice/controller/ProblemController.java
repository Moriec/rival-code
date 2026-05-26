package com.rivalcode.problemservice.controller;

import com.rivalcode.problemservice.api.ProblemApi;
import com.rivalcode.problemservice.service.ProblemService;
import com.rivalcode.contracts.problems.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProblemController implements ProblemApi {

    private final ProblemService problemService;

    @Override
    public Page<ProblemSummaryDto> listProblems(ProblemFilterRequest filter) {
        return problemService.listProblems(filter);
    }

    @Override
    public ProblemDetailsDto getProblemDetails(UUID problemId) {
        return problemService.getProblemDetails(problemId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ProblemDetailsDto createProblem(CreateProblemRequest request) {
        UUID userId = getCurrentUserId();
        return problemService.createProblem(request, userId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ProblemDetailsDto updateProblem(UUID problemId, UpdateProblemRequest request) {
        return problemService.updateProblem(problemId, request);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return UUID.fromString((String) auth.getPrincipal());
    }
}