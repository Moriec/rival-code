package com.rivalcode.duelservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.common.EventEnvelope;
import com.rivalcode.contracts.duels.enums.*;
import com.rivalcode.contracts.duels.model.*;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.contracts.notifications.enums.NotificationType;
import com.rivalcode.contracts.notifications.model.NotificationCommand;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionRequest;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionResponse;
import com.rivalcode.contracts.submissions.model.SubmissionEvaluatedEvent;
import com.rivalcode.duelservice.client.ProblemServiceClient;
import com.rivalcode.duelservice.model.*;
import com.rivalcode.duelservice.repository.*;
import com.rivalcode.starter.events.EventEnvelopeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DuelService {

    private static final String OUTBOX_PENDING = "PENDING";
    private static final String DUEL_FINISHED_EVENT_TYPE = "DUEL_FINISHED";
    private static final String MATCH_FOUND_EVENT_TYPE = "MATCH_FOUND_NOTIFICATION";
    private static final String RATING_CHANGED_EVENT_TYPE = "RATING_CHANGED_NOTIFICATION";

    private final SeasonRepository seasonRepository;
    private final QueuePresetRepository queuePresetRepository;
    private final DuelProblemPoolRepository problemPoolRepository;
    private final DuelProblemPoolItemRepository problemPoolItemRepository;
    private final MatchmakingTicketRepository ticketRepository;
    private final DuelRepository duelRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserDuelProfileRepository profileRepository;
    private final RatingHistoryRepository ratingHistoryRepository;
    private final DuelEventLogRepository eventLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProblemServiceClient problemServiceClient;
    private final DuelMapper duelMapper;
    private final RatingCalculator ratingCalculator;
    private final EventEnvelopeFactory eventEnvelopeFactory;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.duel.ticket-ttl-seconds:300}")
    private long ticketTtlSeconds;

    @Value("${app.duel.default-rating:1200}")
    private int defaultRating;

    @Value("${app.duel.rating-window-step-seconds:30}")
    private long ratingWindowStepSeconds;

    @Value("${app.duel.rating-window-step-points:50}")
    private int ratingWindowStepPoints;

    @Value("${app.kafka.duel-events-topic:duel-events.v1}")
    private String duelEventsTopic;

    @Value("${app.kafka.notification-commands-topic:notification-commands.v1}")
    private String notificationCommandsTopic;

    @Transactional
    public MatchmakingTicketDto createTicket(CreateMatchmakingTicketRequest request,
                                             UUID currentUserId,
                                             String username) {
        validateTicketRequest(request, currentUserId);

        DuelMode mode = request.getMode();
        QueuePresetEntity preset = resolvePreset(request.getPresetId(), mode);
        UserDuelProfileEntity profile = getOrCreateProfile(currentUserId, username);
        int rating = request.getCurrentRating() != null && request.getCurrentRating() > 0
                ? request.getCurrentRating()
                : profile.getRating();

        if (ticketRepository.existsByUserIdAndStatus(currentUserId, MatchmakingTicketStatus.WAITING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a waiting matchmaking ticket");
        }

        Instant now = Instant.now();
        MatchmakingTicketEntity ticket = MatchmakingTicketEntity.builder()
                .ticketId(UUID.randomUUID())
                .userId(currentUserId)
                .presetId(preset.getPresetId())
                .mode(mode)
                .difficulty(request.getDifficulty())
                .status(MatchmakingTicketStatus.WAITING)
                .currentRating(rating)
                .createdAt(now)
                .expiresAt(now.plusSeconds(ticketTtlSeconds))
                .updatedAt(now)
                .build();
        ticketRepository.save(ticket);

        Optional<MatchmakingTicketEntity> opponent = findOpponent(ticket, preset, now);
        opponent.ifPresent(opponentTicket -> matchTickets(ticket, opponentTicket, preset, now));
        return duelMapper.toTicketDto(ticket);
    }

    @Transactional
    public MatchmakingTicketDto cancelTicket(UUID ticketId, UUID currentUserId) {
        MatchmakingTicketEntity ticket = ticketRepository.findByTicketIdAndUserId(ticketId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (ticket.getStatus() != MatchmakingTicketStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only waiting tickets can be cancelled");
        }
        ticket.setStatus(MatchmakingTicketStatus.CANCELLED);
        ticket.setUpdatedAt(Instant.now());
        return duelMapper.toTicketDto(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public MatchmakingTicketDto getTicket(UUID ticketId, UUID currentUserId) {
        MatchmakingTicketEntity ticket = ticketRepository.findByTicketIdAndUserId(ticketId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        return duelMapper.toTicketDto(ticket);
    }

    @Transactional(readOnly = true)
    public DuelRoomStateDto getDuel(UUID duelId) {
        DuelEntity duel = duelRepository.findById(duelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duel not found"));
        return duelMapper.toRoomState(duel, participantRepository.findByDuelIdOrderByCreatedAtAsc(duelId));
    }

    @Transactional(readOnly = true)
    public UserDuelProfileDto getProfile(UUID userId) {
        UserDuelProfileEntity profile = profileRepository.findById(userId)
                .orElseGet(() -> defaultProfile(userId, null));
        int rank = calculateRank(userId);
        List<RecentDuelDto> recentDuels = participantRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toRecentDuel)
                .flatMap(Optional::stream)
                .toList();
        return duelMapper.toUserProfile(profile, rank, recentDuels);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(UUID seasonId) {
        List<UserDuelProfileEntity> profiles = profileRepository.findTop100ByOrderByRatingDescUserIdAsc();
        List<LeaderboardEntryDto> entries = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            entries.add(duelMapper.toLeaderboardEntry(profiles.get(i), i + 1));
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public List<SeasonDto> listSeasons() {
        return seasonRepository.findAllByOrderByStartsAtDesc().stream()
                .map(duelMapper::toSeasonDto)
                .toList();
    }

    @Transactional
    public SeasonDto saveSeason(SeasonDto request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Season name is required");
        }
        SeasonEntity season = SeasonEntity.builder()
                .seasonId(parseOptionalUuid(request.getSeasonId()).orElseGet(UUID::randomUUID))
                .name(request.getName())
                .startsAt(Objects.requireNonNullElseGet(request.getStartsAt(), Instant::now))
                .endsAt(Objects.requireNonNullElseGet(request.getEndsAt(), () -> Instant.now().plusSeconds(31_536_000)))
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive()))
                .build();
        return duelMapper.toSeasonDto(seasonRepository.save(season));
    }

    @Transactional(readOnly = true)
    public List<QueuePresetDto> listQueuePresets() {
        return queuePresetRepository.findAllByOrderByNameAsc().stream()
                .map(duelMapper::toQueuePresetDto)
                .toList();
    }

    @Transactional
    public QueuePresetDto saveQueuePreset(QueuePresetDto request) {
        if (request == null || !StringUtils.hasText(request.getName()) || request.getMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Queue preset name and mode are required");
        }
        QueuePresetEntity preset = QueuePresetEntity.builder()
                .presetId(parseOptionalUuid(request.getPresetId()).orElseGet(UUID::randomUUID))
                .name(request.getName())
                .mode(request.getMode())
                .duelDurationSeconds(toPositiveInt(request.getDuelDurationSeconds(), 900))
                .initialRatingWindow(toPositiveInt(request.getInitialRatingWindow(), 150))
                .maxRatingWindow(toPositiveInt(request.getMaxRatingWindow(), 600))
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive()))
                .build();
        return duelMapper.toQueuePresetDto(queuePresetRepository.save(preset));
    }

    @Transactional(readOnly = true)
    public List<DuelProblemPoolDto> listProblemPools() {
        return problemPoolRepository.findAllByOrderByNameAsc().stream()
                .map(pool -> duelMapper.toProblemPoolDto(pool, problemIdsForPool(pool.getPoolId())))
                .toList();
    }

    @Transactional
    public DuelProblemPoolDto saveProblemPool(DuelProblemPoolDto request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Problem pool name is required");
        }
        UUID poolId = parseOptionalUuid(request.getPoolId()).orElseGet(UUID::randomUUID);
        DuelProblemPoolEntity pool = DuelProblemPoolEntity.builder()
                .poolId(poolId)
                .name(request.getName())
                .seasonId(parseOptionalUuid(request.getSeasonId()).orElse(null))
                .presetId(parseOptionalUuid(request.getPresetId()).orElse(null))
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive()))
                .build();
        problemPoolRepository.save(pool);

        problemPoolItemRepository.deleteById_PoolId(poolId);
        List<UUID> problemIds = request.getProblemIds() == null ? List.of() : request.getProblemIds().stream()
                .filter(StringUtils::hasText)
                .map(this::parseRequiredUuid)
                .toList();
        problemPoolItemRepository.saveAll(problemIds.stream()
                .map(problemId -> DuelProblemPoolItemEntity.builder()
                        .id(DuelProblemPoolItemId.builder()
                                .poolId(poolId)
                                .problemId(problemId)
                                .build())
                        .build())
                .toList());
        return duelMapper.toProblemPoolDto(pool, problemIds);
    }

    @Transactional
    public void handleSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        if (event == null || !StringUtils.hasText(event.getDuelId()) || !Boolean.TRUE.equals(event.getAccepted())) {
            return;
        }

        UUID duelId = parseRequiredUuid(event.getDuelId());
        UUID userId = parseRequiredUuid(event.getUserId());
        UUID submissionId = parseRequiredUuid(event.getSubmissionId());

        DuelEntity duel = duelRepository.findByDuelId(duelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duel not found"));
        if (duel.getStatus() == DuelStatus.FINISHED || duel.getStatus() == DuelStatus.CANCELLED) {
            return;
        }

        DuelParticipantEntity participant = participantRepository.findByDuelIdAndUserId(duelId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission user is not a duel participant"));
        if (participant.getAcceptedSubmissionId() == null) {
            participant.setAcceptedSubmissionId(submissionId);
            participant.setUpdatedAt(Instant.now());
            participantRepository.save(participant);
        }

        finishDuel(duel, userId, Instant.now());
    }

    @Scheduled(fixedDelayString = "${app.duel.expire-delay-ms:5000}")
    @Transactional
    public void expireFinishedByClock() {
        Instant now = Instant.now();
        for (DuelEntity duel : duelRepository.findTop50ByStatusAndEndsAtBeforeOrderByEndsAtAsc(DuelStatus.IN_PROGRESS, now)) {
            duelRepository.findByDuelId(duel.getDuelId())
                    .filter(locked -> locked.getStatus() == DuelStatus.IN_PROGRESS)
                    .ifPresent(locked -> finishDuel(locked, null, now));
        }
    }

    @Scheduled(fixedDelayString = "${app.duel.ticket-expire-delay-ms:10000}")
    @Transactional
    public void expireWaitingTickets() {
        Instant now = Instant.now();
        List<MatchmakingTicketEntity> expired =
                ticketRepository.findByStatusAndExpiresAtBefore(MatchmakingTicketStatus.WAITING, now);
        expired.forEach(ticket -> {
            ticket.setStatus(MatchmakingTicketStatus.EXPIRED);
            ticket.setUpdatedAt(now);
        });
        ticketRepository.saveAll(expired);
    }

    @Scheduled(fixedDelayString = "${app.duel.matchmaking-delay-ms:3000}")
    @Transactional
    public void matchWaitingTickets() {
        Instant now = Instant.now();
        List<MatchmakingTicketEntity> tickets =
                ticketRepository.findTop100ByStatusAndExpiresAtAfterOrderByCreatedAtAsc(
                        MatchmakingTicketStatus.WAITING,
                        now
                );
        for (MatchmakingTicketEntity ticket : tickets) {
            if (ticket.getStatus() != MatchmakingTicketStatus.WAITING || ticket.getMode() != DuelMode.RATED) {
                continue;
            }
            QueuePresetEntity preset = queuePresetRepository.findById(ticket.getPresetId())
                    .filter(QueuePresetEntity::isActive)
                    .orElse(null);
            if (preset == null) {
                continue;
            }
            tickets.stream()
                    .filter(candidate -> candidate.getStatus() == MatchmakingTicketStatus.WAITING)
                    .filter(candidate -> !candidate.getTicketId().equals(ticket.getTicketId()))
                    .filter(candidate -> !candidate.getUserId().equals(ticket.getUserId()))
                    .filter(candidate -> isSameMatchmakingQueue(ticket, candidate))
                    .filter(candidate -> isWithinDynamicRatingWindow(ticket, candidate, preset, now))
                    .findFirst()
                    .ifPresent(candidate -> matchTickets(ticket, candidate, preset, now));
        }
    }

    private void validateTicketRequest(CreateMatchmakingTicketRequest request, UUID currentUserId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is required");
        }
        if (request.getMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duel mode is required");
        }
        if (request.getMode() != DuelMode.RATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty matchmaking supports only rated duels");
        }
        if (request.getDifficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duel difficulty is required");
        }
        Optional<UUID> requestedUserId = parseOptionalUuid(request.getUserId());
        if (requestedUserId.isPresent() && !requestedUserId.get().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create matchmaking ticket for another user");
        }
    }

    private QueuePresetEntity resolvePreset(String presetId, DuelMode mode) {
        if (StringUtils.hasText(presetId)) {
            QueuePresetEntity preset = queuePresetRepository.findById(parseRequiredUuid(presetId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Queue preset not found"));
            if (!preset.isActive() || preset.getMode() != mode) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Queue preset is inactive or has another mode");
            }
            return preset;
        }
        return queuePresetRepository.findFirstByModeAndActiveTrue(mode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active queue preset not found"));
    }

    private Optional<MatchmakingTicketEntity> findOpponent(MatchmakingTicketEntity ticket,
                                                          QueuePresetEntity preset,
                                                          Instant now) {
        return ticketRepository.findTop20ByPresetIdAndModeAndDifficultyAndStatusAndExpiresAtAfterOrderByCreatedAtAsc(
                        preset.getPresetId(),
                        ticket.getMode(),
                        ticket.getDifficulty(),
                        MatchmakingTicketStatus.WAITING,
                        now
                )
                .stream()
                .filter(candidate -> !candidate.getTicketId().equals(ticket.getTicketId()))
                .filter(candidate -> !candidate.getUserId().equals(ticket.getUserId()))
                .filter(candidate -> isWithinDynamicRatingWindow(ticket, candidate, preset, now))
                .findFirst();
    }

    DuelProblemSelectionRequest buildProblemSelectionRequest(MatchmakingTicketEntity first,
                                                             MatchmakingTicketEntity second,
                                                             QueuePresetEntity preset,
                                                             DuelProblemPoolEntity pool) {
        return DuelProblemSelectionRequest.builder()
                .presetId(preset.getPresetId().toString())
                .poolId(pool != null ? pool.getPoolId().toString() : null)
                .userIds(List.of(first.getUserId().toString(), second.getUserId().toString()))
                .difficulties(List.of(first.getDifficulty()))
                .ratedMode(first.getMode() == DuelMode.RATED)
                .build();
    }

    boolean isSameMatchmakingQueue(MatchmakingTicketEntity first, MatchmakingTicketEntity second) {
        return Objects.equals(first.getPresetId(), second.getPresetId())
                && first.getMode() == second.getMode()
                && first.getDifficulty() == second.getDifficulty();
    }

    boolean isWithinDynamicRatingWindow(MatchmakingTicketEntity first,
                                        MatchmakingTicketEntity second,
                                        QueuePresetEntity preset,
                                        Instant now) {
        int ratingDifference = Math.abs(first.getCurrentRating() - second.getCurrentRating());
        return ratingDifference <= dynamicRatingWindow(first, second, preset, now);
    }

    int dynamicRatingWindow(MatchmakingTicketEntity first,
                            MatchmakingTicketEntity second,
                            QueuePresetEntity preset,
                            Instant now) {
        int initialWindow = Math.max(0, preset.getInitialRatingWindow());
        int maxWindow = Math.max(initialWindow, preset.getMaxRatingWindow());
        long firstWaitSeconds = secondsWaiting(first, now);
        long secondWaitSeconds = secondsWaiting(second, now);
        long waitSeconds = Math.max(firstWaitSeconds, secondWaitSeconds);
        long steps = ratingWindowStepSeconds > 0 ? waitSeconds / ratingWindowStepSeconds : 0;
        long expandedWindow = initialWindow + steps * (long) Math.max(0, ratingWindowStepPoints);
        return (int) Math.min(maxWindow, expandedWindow);
    }

    private long secondsWaiting(MatchmakingTicketEntity ticket, Instant now) {
        if (ticket.getCreatedAt() == null || now == null || now.isBefore(ticket.getCreatedAt())) {
            return 0;
        }
        return Duration.between(ticket.getCreatedAt(), now).toSeconds();
    }

    private void matchTickets(MatchmakingTicketEntity first,
                              MatchmakingTicketEntity second,
                              QueuePresetEntity preset,
                              Instant now) {
        SeasonEntity season = seasonRepository.findFirstByActiveTrueOrderByStartsAtDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active season not found"));
        Optional<DuelProblemPoolEntity> pool = problemPoolRepository
                .findFirstBySeasonIdAndPresetIdAndActiveTrue(season.getSeasonId(), preset.getPresetId())
                .or(() -> problemPoolRepository.findFirstByPresetIdAndActiveTrue(preset.getPresetId()));

        DuelProblemSelectionResponse selectedProblem = problemServiceClient.selectProblem(
                buildProblemSelectionRequest(first, second, preset, pool.orElse(null))
        );
        if (selectedProblem == null || !StringUtils.hasText(selectedProblem.getProblemId())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Problem service returned empty duel problem");
        }

        UUID duelId = UUID.randomUUID();
        DuelEntity duel = DuelEntity.builder()
                .duelId(duelId)
                .seasonId(season.getSeasonId())
                .presetId(preset.getPresetId())
                .mode(first.getMode())
                .status(DuelStatus.IN_PROGRESS)
                .problemId(parseRequiredUuid(selectedProblem.getProblemId()))
                .problemVersionId(parseOptionalUuid(selectedProblem.getProblemVersionId()).orElse(null))
                .problemTitle(selectedProblem.getTitle())
                .problemDifficulty(first.getDifficulty())
                .startedAt(now)
                .endsAt(now.plusSeconds(preset.getDuelDurationSeconds()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        duelRepository.save(duel);

        createParticipant(duelId, first, now);
        createParticipant(duelId, second, now);

        first.setStatus(MatchmakingTicketStatus.MATCHED);
        first.setMatchedDuelId(duelId);
        first.setUpdatedAt(now);
        second.setStatus(MatchmakingTicketStatus.MATCHED);
        second.setMatchedDuelId(duelId);
        second.setUpdatedAt(now);
        ticketRepository.saveAll(List.of(first, second));

        logDuelEvent(duelId, "MATCH_FOUND", Map.of(
                "duelId", duelId.toString(),
                "problemId", duel.getProblemId().toString(),
                "participants", List.of(first.getUserId().toString(), second.getUserId().toString())
        ));

        List<DuelParticipantEntity> participants = participantRepository.findByDuelIdOrderByCreatedAtAsc(duelId);
        enqueueMatchFoundNotifications(duel, participants);
        messagingTemplate.convertAndSend("/topic/duels/" + duelId + "/state", duelMapper.toRoomState(duel, participants));
    }

    private void createParticipant(UUID duelId, MatchmakingTicketEntity ticket, Instant now) {
        UserDuelProfileEntity profile = getOrCreateProfile(ticket.getUserId(), null);
        participantRepository.save(DuelParticipantEntity.builder()
                .participantId(UUID.randomUUID())
                .duelId(duelId)
                .userId(ticket.getUserId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .avatarUrl(profile.getAvatarUrl())
                .ratingBefore(ticket.getCurrentRating())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void finishDuel(DuelEntity duel, UUID winnerUserId, Instant finishedAt) {
        if (duel.getStatus() == DuelStatus.FINISHED) {
            return;
        }

        List<DuelParticipantEntity> participants = participantRepository.findByDuelIdOrderByCreatedAtAsc(duel.getDuelId());
        Map<UUID, UserDuelProfileEntity> profiles = participants.stream()
                .map(participant -> getOrCreateProfile(participant.getUserId(), participant.getUsername()))
                .collect(Collectors.toMap(UserDuelProfileEntity::getUserId, Function.identity()));

        for (DuelParticipantEntity participant : participants) {
            DuelOutcome outcome = outcomeFor(participant.getUserId(), winnerUserId);
            participant.setOutcome(outcome);

            UserDuelProfileEntity profile = profiles.get(participant.getUserId());
            int ratingBefore = profile.getRating();
            int ratingDelta = duel.getMode() == DuelMode.RATED ? ratingCalculator.delta(outcome) : 0;
            int ratingAfter = Math.max(0, ratingBefore + ratingDelta);

            applyProfileResult(profile, outcome, ratingAfter, participant.getUserId().equals(winnerUserId));
            participant.setRatingAfter(ratingAfter);
            participant.setUpdatedAt(finishedAt);
            profileRepository.save(profile);

            if (duel.getMode() == DuelMode.RATED) {
                ratingHistoryRepository.save(RatingHistoryEntity.builder()
                        .ratingHistoryId(UUID.randomUUID())
                        .userId(participant.getUserId())
                        .duelId(duel.getDuelId())
                        .seasonId(duel.getSeasonId())
                        .oldRating(ratingBefore)
                        .newRating(ratingAfter)
                        .delta(ratingAfter - ratingBefore)
                        .reason(reasonFor(outcome))
                        .changedAt(finishedAt)
                        .build());
            }
        }
        participantRepository.saveAll(participants);

        duel.setStatus(DuelStatus.FINISHED);
        duel.setWinnerUserId(winnerUserId);
        duel.setFinishedAt(finishedAt);
        duel.setUpdatedAt(finishedAt);
        duelRepository.save(duel);

        DuelFinishedEvent event = buildDuelFinishedEvent(duel, participants);
        logDuelEvent(duel.getDuelId(), DUEL_FINISHED_EVENT_TYPE, event);
        enqueueDuelFinishedEvent(duel, event);
        enqueueDuelFinishedNotifications(duel, participants);
        enqueueRatingChangedNotifications(duel, participants);

        messagingTemplate.convertAndSend("/topic/duels/" + duel.getDuelId() + "/finished", event);
        messagingTemplate.convertAndSend("/topic/duels/" + duel.getDuelId() + "/state", duelMapper.toRoomState(duel, participants));
    }

    private DuelOutcome outcomeFor(UUID participantUserId, UUID winnerUserId) {
        if (winnerUserId == null) {
            return DuelOutcome.DRAW;
        }
        return participantUserId.equals(winnerUserId) ? DuelOutcome.WIN : DuelOutcome.LOSS;
    }

    private void applyProfileResult(UserDuelProfileEntity profile,
                                    DuelOutcome outcome,
                                    int ratingAfter,
                                    boolean solvedProblem) {
        profile.setRating(ratingAfter);
        profile.setUpdatedAt(Instant.now());
        switch (outcome) {
            case WIN -> profile.setWins(profile.getWins() + 1);
            case LOSS -> profile.setLosses(profile.getLosses() + 1);
            case DRAW -> profile.setDraws(profile.getDraws() + 1);
            case CANCELLED -> {
            }
        }
        if (solvedProblem) {
            profile.setSolvedProblems(profile.getSolvedProblems() + 1);
        }
    }

    private RatingChangeReason reasonFor(DuelOutcome outcome) {
        return switch (outcome) {
            case WIN -> RatingChangeReason.DUEL_WIN;
            case LOSS -> RatingChangeReason.DUEL_LOSS;
            case DRAW, CANCELLED -> RatingChangeReason.DUEL_DRAW;
        };
    }

    private DuelFinishedEvent buildDuelFinishedEvent(DuelEntity duel, List<DuelParticipantEntity> participants) {
        return DuelFinishedEvent.builder()
                .duelId(duel.getDuelId().toString())
                .mode(duel.getMode())
                .winnerUserId(duel.getWinnerUserId() != null ? duel.getWinnerUserId().toString() : null)
                .problemId(duel.getProblemId().toString())
                .problemVersionId(duel.getProblemVersionId() != null ? duel.getProblemVersionId().toString() : null)
                .participants(participants.stream().map(duelMapper::toParticipantDto).toList())
                .finishedAt(duel.getFinishedAt())
                .build();
    }

    private void enqueueDuelFinishedEvent(DuelEntity duel, DuelFinishedEvent event) {
        EventEnvelope<DuelFinishedEvent> envelope = eventEnvelopeFactory.create(DUEL_FINISHED_EVENT_TYPE, 1, event);
        enqueueOutbox(DUEL_FINISHED_EVENT_TYPE, duel.getDuelId(), duelEventsTopic, duel.getDuelId().toString(), envelope);
    }

    private void enqueueMatchFoundNotifications(DuelEntity duel, List<DuelParticipantEntity> participants) {
        for (DuelParticipantEntity participant : participants) {
            DuelParticipantEntity opponent = opponentOf(participants, participant.getUserId()).orElse(null);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("duelId", duel.getDuelId().toString());
            payload.put("problemId", duel.getProblemId().toString());
            payload.put("problemTitle", duel.getProblemTitle());
            payload.put("opponentUserId", opponent != null ? opponent.getUserId().toString() : null);

            NotificationCommand command = NotificationCommand.builder()
                    .notificationId(UUID.randomUUID())
                    .userId(participant.getUserId().toString())
                    .type(NotificationType.MATCH_FOUND)
                    .title("Duel match found")
                    .body("Your duel is ready.")
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();
            enqueueOutbox(MATCH_FOUND_EVENT_TYPE, duel.getDuelId(), notificationCommandsTopic, participant.getUserId().toString(), command);
        }
    }

    private void enqueueDuelFinishedNotifications(DuelEntity duel, List<DuelParticipantEntity> participants) {
        for (DuelParticipantEntity participant : participants) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("duelId", duel.getDuelId().toString());
            payload.put("outcome", participant.getOutcome().name());
            payload.put("winnerUserId", duel.getWinnerUserId() != null ? duel.getWinnerUserId().toString() : null);

            NotificationCommand command = NotificationCommand.builder()
                    .notificationId(UUID.randomUUID())
                    .userId(participant.getUserId().toString())
                    .type(NotificationType.DUEL_FINISHED)
                    .title("Duel finished")
                    .body("Your duel has finished.")
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();
            enqueueOutbox(DUEL_FINISHED_EVENT_TYPE + "_NOTIFICATION", duel.getDuelId(), notificationCommandsTopic, participant.getUserId().toString(), command);
        }
    }

    private void enqueueRatingChangedNotifications(DuelEntity duel, List<DuelParticipantEntity> participants) {
        if (duel.getMode() != DuelMode.RATED) {
            return;
        }
        for (DuelParticipantEntity participant : participants) {
            int before = participant.getRatingBefore();
            int after = participant.getRatingAfter() != null ? participant.getRatingAfter() : before;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("duelId", duel.getDuelId().toString());
            payload.put("oldRating", before);
            payload.put("newRating", after);
            payload.put("delta", after - before);

            NotificationCommand command = NotificationCommand.builder()
                    .notificationId(UUID.randomUUID())
                    .userId(participant.getUserId().toString())
                    .type(NotificationType.RATING_CHANGED)
                    .title("Rating changed")
                    .body("Your duel rating has been updated.")
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();
            enqueueOutbox(RATING_CHANGED_EVENT_TYPE, duel.getDuelId(), notificationCommandsTopic, participant.getUserId().toString(), command);
        }
    }

    private void enqueueOutbox(String eventType, UUID aggregateId, String topic, String key, Object payload) {
        outboxEventRepository.save(OutboxEventEntity.builder()
                .outboxEventId(UUID.randomUUID())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .topic(topic)
                .messageKey(key)
                .payloadJson(objectMapper.valueToTree(payload))
                .status(OUTBOX_PENDING)
                .createdAt(Instant.now())
                .build());
    }

    private void logDuelEvent(UUID duelId, String eventType, Object payload) {
        JsonNode payloadJson = objectMapper.valueToTree(payload);
        eventLogRepository.save(DuelEventLogEntity.builder()
                .eventId(UUID.randomUUID())
                .duelId(duelId)
                .eventType(eventType)
                .payload(payloadJson)
                .createdAt(Instant.now())
                .build());
    }

    private UserDuelProfileEntity getOrCreateProfile(UUID userId, String username) {
        return profileRepository.findById(userId)
                .map(profile -> {
                    if (StringUtils.hasText(username) && !StringUtils.hasText(profile.getUsername())) {
                        profile.setUsername(username);
                        profile.setUpdatedAt(Instant.now());
                        return profileRepository.save(profile);
                    }
                    return profile;
                })
                .orElseGet(() -> profileRepository.save(defaultProfile(userId, username)));
    }

    private UserDuelProfileEntity defaultProfile(UUID userId, String username) {
        return UserDuelProfileEntity.builder()
                .userId(userId)
                .username(username)
                .rating(defaultRating)
                .wins(0)
                .losses(0)
                .draws(0)
                .solvedProblems(0)
                .updatedAt(Instant.now())
                .build();
    }

    private int calculateRank(UUID userId) {
        List<UserDuelProfileEntity> profiles = profileRepository.findTop100ByOrderByRatingDescUserIdAsc();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }
        return profiles.size() + 1;
    }

    private Optional<RecentDuelDto> toRecentDuel(DuelParticipantEntity participant) {
        return duelRepository.findById(participant.getDuelId()).map(duel -> {
            List<DuelParticipantEntity> participants = participantRepository.findByDuelIdOrderByCreatedAtAsc(duel.getDuelId());
            DuelParticipantEntity opponent = opponentOf(participants, participant.getUserId()).orElse(null);
            int ratingDelta = participant.getRatingAfter() != null
                    ? participant.getRatingAfter() - participant.getRatingBefore()
                    : 0;
            return RecentDuelDto.builder()
                    .duelId(duel.getDuelId().toString())
                    .opponentUserId(opponent != null ? opponent.getUserId().toString() : null)
                    .opponentUsername(opponent != null ? opponent.getUsername() : null)
                    .problemId(duel.getProblemId().toString())
                    .problemTitle(duel.getProblemTitle())
                    .problemDifficulty(duel.getProblemDifficulty())
                    .mode(duel.getMode())
                    .outcome(participant.getOutcome())
                    .ratingDelta(ratingDelta)
                    .finishedAt(duel.getFinishedAt())
                    .build();
        });
    }

    private Optional<DuelParticipantEntity> opponentOf(List<DuelParticipantEntity> participants, UUID userId) {
        return participants.stream()
                .filter(candidate -> !candidate.getUserId().equals(userId))
                .findFirst();
    }

    private List<UUID> problemIdsForPool(UUID poolId) {
        return problemPoolItemRepository.findById_PoolId(poolId).stream()
                .map(item -> item.getId().getProblemId())
                .toList();
    }

    private int toPositiveInt(Long value, int defaultValue) {
        if (value == null || value <= 0 || value > Integer.MAX_VALUE) {
            return defaultValue;
        }
        return value.intValue();
    }

    private int toPositiveInt(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private Optional<UUID> parseOptionalUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(parseRequiredUuid(value));
    }

    private UUID parseRequiredUuid(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UUID value is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID: " + value, e);
        }
    }
}
