package com.rivalcode.duelservice.model;

import com.rivalcode.contracts.duels.enums.DuelOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "duel_participants", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelParticipantEntity {

    @Id
    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "duel_id", nullable = false)
    private UUID duelId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 128)
    private String username;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "rating_before", nullable = false)
    private int ratingBefore;

    @Column(name = "rating_after")
    private Integer ratingAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private DuelOutcome outcome;

    @Column(name = "accepted_submission_id")
    private UUID acceptedSubmissionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
