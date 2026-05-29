package com.rivalcode.duelservice.model;

import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.duels.enums.DuelStatus;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "duels", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelEntity {

    @Id
    @Column(name = "duel_id")
    private UUID duelId;

    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "preset_id")
    private UUID presetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DuelMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DuelStatus status;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "problem_version_id")
    private UUID problemVersionId;

    @Column(name = "problem_title", length = 256)
    private String problemTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_difficulty", nullable = false, length = 32)
    private ProblemDifficulty problemDifficulty;

    @Column(name = "winner_user_id")
    private UUID winnerUserId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
