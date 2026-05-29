package com.rivalcode.duelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.duels.enums.MatchmakingTicketStatus;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionRequest;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.duelservice.client.ProblemServiceClient;
import com.rivalcode.duelservice.model.DuelProblemPoolEntity;
import com.rivalcode.duelservice.model.MatchmakingTicketEntity;
import com.rivalcode.duelservice.model.QueuePresetEntity;
import com.rivalcode.duelservice.repository.*;
import com.rivalcode.starter.events.EventEnvelopeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DuelServiceMatchmakingTest {

    private DuelService duelService;

    @BeforeEach
    void setUp() {
        duelService = new DuelService(
                mock(SeasonRepository.class),
                mock(QueuePresetRepository.class),
                mock(DuelProblemPoolRepository.class),
                mock(DuelProblemPoolItemRepository.class),
                mock(MatchmakingTicketRepository.class),
                mock(DuelRepository.class),
                mock(DuelParticipantRepository.class),
                mock(UserDuelProfileRepository.class),
                mock(RatingHistoryRepository.class),
                mock(DuelEventLogRepository.class),
                mock(OutboxEventRepository.class),
                mock(ProblemServiceClient.class),
                new DuelMapper(),
                new RatingCalculator(),
                mock(EventEnvelopeFactory.class),
                new ObjectMapper(),
                mock(SimpMessagingTemplate.class)
        );
        ReflectionTestUtils.setField(duelService, "ratingWindowStepSeconds", 30L);
        ReflectionTestUtils.setField(duelService, "ratingWindowStepPoints", 50);
    }

    @Test
    void ticketsWithDifferentDifficultyAreDifferentQueues() {
        UUID presetId = UUID.randomUUID();
        MatchmakingTicketEntity easy = ticket(presetId, ProblemDifficulty.EASY, 1200, Instant.now());
        MatchmakingTicketEntity medium = ticket(presetId, ProblemDifficulty.MEDIUM, 1200, Instant.now());

        assertThat(duelService.isSameMatchmakingQueue(easy, medium)).isFalse();
    }

    @Test
    void dynamicRatingWindowExpandsWithWaitingTimeAndCapsAtMax() {
        Instant now = Instant.parse("2026-01-01T00:02:00Z");
        UUID presetId = UUID.randomUUID();
        QueuePresetEntity preset = QueuePresetEntity.builder()
                .presetId(presetId)
                .mode(DuelMode.RATED)
                .initialRatingWindow(150)
                .maxRatingWindow(250)
                .active(true)
                .build();
        MatchmakingTicketEntity longWaiting = ticket(presetId, ProblemDifficulty.HARD, 1200, now.minusSeconds(120));
        MatchmakingTicketEntity fresh = ticket(presetId, ProblemDifficulty.HARD, 1450, now.minusSeconds(5));
        MatchmakingTicketEntity tooFar = ticket(presetId, ProblemDifficulty.HARD, 1451, now.minusSeconds(5));

        assertThat(duelService.dynamicRatingWindow(longWaiting, fresh, preset, now)).isEqualTo(250);
        assertThat(duelService.isWithinDynamicRatingWindow(longWaiting, fresh, preset, now)).isTrue();
        assertThat(duelService.isWithinDynamicRatingWindow(longWaiting, tooFar, preset, now)).isFalse();
    }

    @Test
    void problemSelectionRequestCarriesSelectedDifficulty() {
        UUID presetId = UUID.randomUUID();
        UUID poolId = UUID.randomUUID();
        QueuePresetEntity preset = QueuePresetEntity.builder()
                .presetId(presetId)
                .mode(DuelMode.RATED)
                .build();
        DuelProblemPoolEntity pool = DuelProblemPoolEntity.builder()
                .poolId(poolId)
                .presetId(presetId)
                .active(true)
                .build();
        MatchmakingTicketEntity first = ticket(presetId, ProblemDifficulty.EASY, 1200, Instant.now());
        MatchmakingTicketEntity second = ticket(presetId, ProblemDifficulty.EASY, 1210, Instant.now());

        DuelProblemSelectionRequest request = duelService.buildProblemSelectionRequest(first, second, preset, pool);

        assertThat(request.getPresetId()).isEqualTo(presetId.toString());
        assertThat(request.getPoolId()).isEqualTo(poolId.toString());
        assertThat(request.getDifficulties()).containsExactly(ProblemDifficulty.EASY);
        assertThat(request.getRatedMode()).isTrue();
    }

    private MatchmakingTicketEntity ticket(
            UUID presetId,
            ProblemDifficulty difficulty,
            int rating,
            Instant createdAt
    ) {
        return MatchmakingTicketEntity.builder()
                .ticketId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .presetId(presetId)
                .mode(DuelMode.RATED)
                .difficulty(difficulty)
                .status(MatchmakingTicketStatus.WAITING)
                .currentRating(rating)
                .createdAt(createdAt)
                .expiresAt(createdAt.plusSeconds(300))
                .updatedAt(createdAt)
                .build();
    }
}
