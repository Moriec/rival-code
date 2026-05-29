package com.rivalcode.duelservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DuelProblemPoolItemId implements Serializable {

    @Column(name = "pool_id")
    private UUID poolId;

    @Column(name = "problem_id")
    private UUID problemId;
}
