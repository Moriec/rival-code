package com.rivalcode.duelservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "duel_problem_pools", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelProblemPoolEntity {

    @Id
    @Column(name = "pool_id")
    private UUID poolId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "preset_id")
    private UUID presetId;

    @Column(nullable = false)
    private boolean active;
}
