package com.rivalcode.duelservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_duel_profiles", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDuelProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 128)
    private String username;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int draws;

    @Column(name = "solved_problems", nullable = false)
    private int solvedProblems;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
