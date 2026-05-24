package com.vinogradov.onlinejudge.service.language;

import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;

import java.util.List;

public interface LanguageProfile {
    ProgrammingLanguages language();
    String sourceFileName();
    boolean requiresCompilation();
    String[] compileCommand();
    String executableFileName();
    List<String> executionCommand(long memoryLimitKb);
    boolean useIsolateMem();
    int processLimit();
    List<String> extraDirMounts();
}
