package com.rivalcode.duelservice.controller;

import com.rivalcode.contracts.duels.model.*;
import com.rivalcode.duelservice.api.DuelApi;
import com.rivalcode.duelservice.service.DuelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DuelController implements DuelApi {

    private final DuelService duelService;

    @Override
    public MatchmakingTicketDto createTicket(CreateMatchmakingTicketRequest request) {
        return duelService.createTicket(request, getCurrentUserId(), getCurrentUsername());
    }

    @Override
    public MatchmakingTicketDto cancelTicket(UUID ticketId) {
        return duelService.cancelTicket(ticketId, getCurrentUserId());
    }

    @Override
    public MatchmakingTicketDto getTicket(UUID ticketId) {
        return duelService.getTicket(ticketId, getCurrentUserId());
    }

    @Override
    public DuelRoomStateDto getDuel(UUID duelId) {
        return duelService.getDuel(duelId);
    }

    @Override
    public UserDuelProfileDto getProfile(UUID userId) {
        return duelService.getProfile(userId);
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard(UUID seasonId) {
        return duelService.getLeaderboard(seasonId);
    }

    @Override
    public List<SeasonDto> listSeasons() {
        return duelService.listSeasons();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public SeasonDto saveSeason(SeasonDto request) {
        return duelService.saveSeason(request);
    }

    @Override
    public List<QueuePresetDto> listQueuePresets() {
        return duelService.listQueuePresets();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public QueuePresetDto saveQueuePreset(QueuePresetDto request) {
        return duelService.saveQueuePreset(request);
    }

    @Override
    public List<DuelProblemPoolDto> listProblemPools() {
        return duelService.listProblemPools();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public DuelProblemPoolDto saveProblemPool(DuelProblemPoolDto request) {
        return duelService.saveProblemPool(request);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return UUID.fromString(userId);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof String username) {
            return username;
        }
        return null;
    }
}
