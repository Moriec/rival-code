package com.rivalcode.problemservice.controller;

import com.rivalcode.problemservice.service.DuelProblemSelector;
import com.rivalcode.problemservice.service.ProblemService;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionRequest;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionResponse;
import com.rivalcode.contracts.problems.model.ProblemExecutionContextDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/problems")
@RequiredArgsConstructor
@Tag(name = "Internal Problems", description = "Internal problem-service operations for backend services")
public class InternalProblemController {

    private final DuelProblemSelector duelProblemSelector;
    private final ProblemService problemService;

    @PostMapping("/select-for-duel")
    @Operation(summary = "Select a problem for duel matchmaking")
    public DuelProblemSelectionResponse selectForDuel(@RequestBody DuelProblemSelectionRequest request) {
        return duelProblemSelector.select(request);
    }

    @GetMapping("/{problemId}/execution-context")
    @Operation(summary = "Get immutable execution context for submission-service")
    public ProblemExecutionContextDto getExecutionContext(
            @PathVariable UUID problemId,
            @RequestParam(required = false) UUID problemVersionId
    ) {
        return problemService.getExecutionContext(problemId, problemVersionId);
    }
}
