package com.vinogradov.contracts.duels.model;

import com.vinogradov.contracts.duels.enums.DuelMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuePresetDto {
    private String presetId;
    private String name;
    private DuelMode mode;
    private Long duelDurationSeconds;
    private Integer initialRatingWindow;
    private Integer maxRatingWindow;
    private Boolean active;
}
