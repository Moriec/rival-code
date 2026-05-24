package com.vinogradov.onlinejudge.model;

import com.vinogradov.onlinejudge.enums.ProgrammingLanguages;
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
