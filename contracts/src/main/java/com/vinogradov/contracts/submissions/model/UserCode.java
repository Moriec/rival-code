package com.vinogradov.contracts.submissions.model;

import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;
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
