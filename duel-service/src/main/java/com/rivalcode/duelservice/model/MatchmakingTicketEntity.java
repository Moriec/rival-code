package com.rivalcode.duelservice.model;

import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.duels.enums.MatchmakingTicketStatus;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matchmaking_tickets", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingTicketEntity {

    @Id
    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "preset_id", nullable = false)
    private UUID presetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DuelMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProblemDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MatchmakingTicketStatus status;

    @Column(name = "current_rating", nullable = false)
    private int currentRating;

    @Column(name = "matched_duel_id")
    private UUID matchedDuelId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
