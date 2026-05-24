package com.rivalcode.onlinejudge.model;

import com.rivalcode.onlinejudge.service.language.LanguageProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@Getter
@AllArgsConstructor
public class CompilationResult {
    private final File executable;
    private final LanguageProfile profile;
}
