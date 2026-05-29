package com.rivalcode.duelservice.repository;

import com.rivalcode.contracts.duels.enums.MatchmakingTicketStatus;
import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import com.rivalcode.duelservice.model.MatchmakingTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchmakingTicketRepository extends JpaRepository<MatchmakingTicketEntity, UUID> {

    boolean existsByUserIdAndStatus(UUID userId, MatchmakingTicketStatus status);

    Optional<MatchmakingTicketEntity> findByTicketIdAndUserId(UUID ticketId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MatchmakingTicketEntity> findTop20ByPresetIdAndModeAndDifficultyAndStatusAndExpiresAtAfterOrderByCreatedAtAsc(
            UUID presetId,
            DuelMode mode,
            ProblemDifficulty difficulty,
            MatchmakingTicketStatus status,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MatchmakingTicketEntity> findTop100ByStatusAndExpiresAtAfterOrderByCreatedAtAsc(
            MatchmakingTicketStatus status,
            Instant now
    );

    List<MatchmakingTicketEntity> findByStatusAndExpiresAtBefore(MatchmakingTicketStatus status, Instant now);
}
