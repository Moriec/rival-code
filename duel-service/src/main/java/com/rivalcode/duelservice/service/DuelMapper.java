package com.rivalcode.duelservice.service;

import com.rivalcode.contracts.duels.model.*;
import com.rivalcode.duelservice.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DuelMapper {

    public MatchmakingTicketDto toTicketDto(MatchmakingTicketEntity ticket) {
        return MatchmakingTicketDto.builder()
                .ticketId(toString(ticket.getTicketId()))
                .userId(toString(ticket.getUserId()))
                .presetId(toString(ticket.getPresetId()))
                .matchedDuelId(toString(ticket.getMatchedDuelId()))
                .mode(ticket.getMode())
                .difficulty(ticket.getDifficulty())
                .status(ticket.getStatus())
                .currentRating(ticket.getCurrentRating())
                .createdAt(ticket.getCreatedAt())
                .expiresAt(ticket.getExpiresAt())
                .build();
    }

    public DuelRoomStateDto toRoomState(DuelEntity duel, List<DuelParticipantEntity> participants) {
        return DuelRoomStateDto.builder()
                .duelId(toString(duel.getDuelId()))
                .status(duel.getStatus())
                .mode(duel.getMode())
                .problemId(toString(duel.getProblemId()))
                .problemVersionId(toString(duel.getProblemVersionId()))
                .problemTitle(duel.getProblemTitle())
                .problemDifficulty(duel.getProblemDifficulty())
                .startedAt(duel.getStartedAt())
                .endsAt(duel.getEndsAt())
                .serverTime(Instant.now())
                .participants(participants.stream().map(this::toParticipantDto).toList())
                .build();
    }

    public DuelParticipantDto toParticipantDto(DuelParticipantEntity participant) {
        return DuelParticipantDto.builder()
                .userId(toString(participant.getUserId()))
                .username(participant.getUsername())
                .displayName(participant.getDisplayName())
                .avatarUrl(participant.getAvatarUrl())
                .ratingBefore(participant.getRatingBefore())
                .ratingAfter(participant.getRatingAfter())
                .outcome(participant.getOutcome())
                .acceptedSubmissionId(toString(participant.getAcceptedSubmissionId()))
                .build();
    }

    public SeasonDto toSeasonDto(SeasonEntity season) {
        return SeasonDto.builder()
                .seasonId(toString(season.getSeasonId()))
                .name(season.getName())
                .startsAt(season.getStartsAt())
                .endsAt(season.getEndsAt())
                .active(season.isActive())
                .build();
    }

    public QueuePresetDto toQueuePresetDto(QueuePresetEntity preset) {
        return QueuePresetDto.builder()
                .presetId(toString(preset.getPresetId()))
                .name(preset.getName())
                .mode(preset.getMode())
                .duelDurationSeconds((long) preset.getDuelDurationSeconds())
                .initialRatingWindow(preset.getInitialRatingWindow())
                .maxRatingWindow(preset.getMaxRatingWindow())
                .active(preset.isActive())
                .build();
    }

    public DuelProblemPoolDto toProblemPoolDto(DuelProblemPoolEntity pool, List<UUID> problemIds) {
        return DuelProblemPoolDto.builder()
                .poolId(toString(pool.getPoolId()))
                .name(pool.getName())
                .seasonId(toString(pool.getSeasonId()))
                .presetId(toString(pool.getPresetId()))
                .problemIds(problemIds.stream().map(UUID::toString).toList())
                .active(pool.isActive())
                .build();
    }

    public LeaderboardEntryDto toLeaderboardEntry(UserDuelProfileEntity profile, int rank) {
        return LeaderboardEntryDto.builder()
                .userId(toString(profile.getUserId()))
                .username(profile.getUsername())
                .avatarUrl(profile.getAvatarUrl())
                .rating(profile.getRating())
                .rank(rank)
                .wins(profile.getWins())
                .losses(profile.getLosses())
                .draws(profile.getDraws())
                .build();
    }

    public UserDuelProfileDto toUserProfile(UserDuelProfileEntity profile,
                                            int rank,
                                            List<RecentDuelDto> recentDuels) {
        return UserDuelProfileDto.builder()
                .userId(toString(profile.getUserId()))
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .avatarUrl(profile.getAvatarUrl())
                .rating(profile.getRating())
                .rank(rank)
                .wins(profile.getWins())
                .losses(profile.getLosses())
                .draws(profile.getDraws())
                .solvedProblems(profile.getSolvedProblems())
                .recentDuels(recentDuels)
                .build();
    }

    public String toString(UUID id) {
        return id != null ? id.toString() : null;
    }
}
