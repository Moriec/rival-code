package com.rivalcode.duelservice.model;

import com.rivalcode.contracts.duels.enums.DuelMode;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "queue_presets", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuePresetEntity {

    @Id
    @Column(name = "preset_id")
    private UUID presetId;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DuelMode mode;

    @Column(name = "duel_duration_seconds", nullable = false)
    private int duelDurationSeconds;

    @Column(name = "initial_rating_window", nullable = false)
    private int initialRatingWindow;

    @Column(name = "max_rating_window", nullable = false)
    private int maxRatingWindow;

    @Column(nullable = false)
    private boolean active;
}
