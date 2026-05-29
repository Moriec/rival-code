package com.rivalcode.contracts.submissions.model;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCode {
    private String sourceCode;
    private ProgrammingLanguages language;
}
