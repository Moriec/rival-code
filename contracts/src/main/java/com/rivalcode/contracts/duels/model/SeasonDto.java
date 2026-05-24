package com.rivalcode.contracts.duels.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonDto {
    private String seasonId;
    private String name;
    private Instant startsAt;
    private Instant endsAt;
    private Boolean active;
}
