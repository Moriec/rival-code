package com.rivalcode.contracts.duels.model;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSnapshotMessage {
    private String duelId;
    private String userId;
    private ProgrammingLanguages language;
    private String sourceCode;
    private Long version;
    private Instant sentAt;
}
