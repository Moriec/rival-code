package com.rivalcode.duelservice.model;

import com.rivalcode.contracts.duels.enums.RatingChangeReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rating_history", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingHistoryEntity {

    @Id
    @Column(name = "rating_history_id")
    private UUID ratingHistoryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "duel_id")
    private UUID duelId;

    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "old_rating", nullable = false)
    private int oldRating;

    @Column(name = "new_rating", nullable = false)
    private int newRating;

    @Column(name = "delta_value", nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private RatingChangeReason reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
