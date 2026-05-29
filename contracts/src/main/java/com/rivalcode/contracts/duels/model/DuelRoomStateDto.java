package com.rivalcode.contracts.duels.model;

import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.contracts.duels.enums.DuelStatus;
import com.rivalcode.contracts.problems.enums.ProblemDifficulty;
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
    private ProblemDifficulty problemDifficulty;
    private Instant startedAt;
    private Instant endsAt;
    private Instant serverTime;
    private List<DuelParticipantDto> participants;
}
