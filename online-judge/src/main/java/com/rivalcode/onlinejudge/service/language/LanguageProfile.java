package com.rivalcode.onlinejudge.service.language;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;

import java.util.List;

public interface LanguageProfile {
    ProgrammingLanguages language();
    String sourceFileName();
    boolean requiresCompilation();
    String[] compileCommand();
    String executableFileName();
    List<String> executionCommand(long memoryLimitKb);
    default long isolateMemoryLimitKb(long memoryLimitKb) {
        return memoryLimitKb;
    }
    boolean useIsolateMem();
    int processLimit();
    List<String> extraDirMounts();
}
