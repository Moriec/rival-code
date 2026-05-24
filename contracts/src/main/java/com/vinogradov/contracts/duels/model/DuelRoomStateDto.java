package com.vinogradov.contracts.duels.model;

import com.vinogradov.contracts.duels.enums.DuelMode;
import com.vinogradov.contracts.duels.enums.DuelStatus;
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
public class DuelRoomStateDto {
    private String duelId;
    private DuelStatus status;
    private DuelMode mode;
    private String problemId;
    private String problemVersionId;
    private String problemTitle;
    private Instant startedAt;
    private Instant endsAt;
    private Instant serverTime;
    private List<DuelParticipantDto> participants;
}
