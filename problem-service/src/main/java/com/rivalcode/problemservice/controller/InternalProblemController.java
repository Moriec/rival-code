package com.rivalcode.problemservice.controller;

import com.rivalcode.problemservice.service.DuelProblemSelector;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionRequest;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/internal/problems")
@RequiredArgsConstructor
public class InternalProblemController {

    private final DuelProblemSelector duelProblemSelector;

    @PostMapping("/select-for-duel")
    public DuelProblemSelectionResponse selectForDuel(@RequestBody DuelProblemSelectionRequest request) {
        return duelProblemSelector.select(request);
    }
}