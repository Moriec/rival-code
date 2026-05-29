package com.rivalcode.duelservice.api;

import com.rivalcode.contracts.duels.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/duels")
@Tag(name = "Duels", description = "Matchmaking, duel rooms, rating profiles and leaderboard")
public interface DuelApi {

    @PostMapping("/matchmaking/tickets")
    @Operation(summary = "Create matchmaking ticket")
    MatchmakingTicketDto createTicket(@Valid @RequestBody CreateMatchmakingTicketRequest request);

    @DeleteMapping("/matchmaking/tickets/{ticketId}")
    @Operation(summary = "Cancel own matchmaking ticket")
    MatchmakingTicketDto cancelTicket(@PathVariable UUID ticketId);

    @GetMapping("/matchmaking/tickets/{ticketId}")
    @Operation(summary = "Get own matchmaking ticket")
    MatchmakingTicketDto getTicket(@PathVariable UUID ticketId);

    @GetMapping("/{duelId}")
    @Operation(summary = "Get duel room state")
    DuelRoomStateDto getDuel(@PathVariable UUID duelId);

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user duel profile")
    UserDuelProfileDto getProfile(@PathVariable UUID userId);

    @GetMapping("/leaderboard")
    @Operation(summary = "Get rating leaderboard")
    List<LeaderboardEntryDto> getLeaderboard(@RequestParam(required = false) UUID seasonId);

    @GetMapping("/seasons")
    @Operation(summary = "List duel seasons")
    List<SeasonDto> listSeasons();

    @PostMapping("/seasons")
    @Operation(summary = "Create or update duel season")
    SeasonDto saveSeason(@Valid @RequestBody SeasonDto request);

    @GetMapping("/queue-presets")
    @Operation(summary = "List queue presets")
    List<QueuePresetDto> listQueuePresets();

    @PostMapping("/queue-presets")
    @Operation(summary = "Create or update queue preset")
    QueuePresetDto saveQueuePreset(@Valid @RequestBody QueuePresetDto request);

    @GetMapping("/problem-pools")
    @Operation(summary = "List duel problem pools")
    List<DuelProblemPoolDto> listProblemPools();

    @PostMapping("/problem-pools")
    @Operation(summary = "Create or update duel problem pool")
    DuelProblemPoolDto saveProblemPool(@Valid @RequestBody DuelProblemPoolDto request);
}
