package com.vinogradov.contracts.duels.model;

import com.vinogradov.contracts.duels.enums.DuelMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelFinishedEvent {
    private String duelId;
    private DuelMode mode;
    private String winnerUserId;
    private String problemId;
    private String problemVersionId;
    private List<DuelParticipantDto> participants;
    private Instant finishedAt;
}
