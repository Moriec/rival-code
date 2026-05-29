package com.rivalcode.duelservice.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "duel_problem_pool_items", schema = "duel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelProblemPoolItemEntity {

    @EmbeddedId
    private DuelProblemPoolItemId id;
}
