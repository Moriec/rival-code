package com.rivalcode.duelservice.service;

import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.duels.enums.MatchmakingTicketStatus;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.duelservice.model.MatchmakingTicketEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DuelMapperTest {

    private final DuelMapper mapper = new DuelMapper();

    @Test
    void mapsTicketWithoutChangingContractIds() {
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID presetId = UUID.randomUUID();
        UUID duelId = UUID.randomUUID();
        Instant now = Instant.now();

        var dto = mapper.toTicketDto(MatchmakingTicketEntity.builder()
                .ticketId(ticketId)
                .userId(userId)
                .presetId(presetId)
                .matchedDuelId(duelId)
                .mode(DuelMode.RATED)
                .difficulty(ProblemDifficulty.EASY)
                .status(MatchmakingTicketStatus.WAITING)
                .currentRating(1200)
                .createdAt(now)
                .expiresAt(now.plusSeconds(300))
                .build());

        assertThat(dto.getTicketId()).isEqualTo(ticketId.toString());
        assertThat(dto.getUserId()).isEqualTo(userId.toString());
        assertThat(dto.getPresetId()).isEqualTo(presetId.toString());
        assertThat(dto.getMatchedDuelId()).isEqualTo(duelId.toString());
        assertThat(dto.getMode()).isEqualTo(DuelMode.RATED);
        assertThat(dto.getDifficulty()).isEqualTo(ProblemDifficulty.EASY);
        assertThat(dto.getStatus()).isEqualTo(MatchmakingTicketStatus.WAITING);
        assertThat(dto.getCurrentRating()).isEqualTo(1200);
    }
}
