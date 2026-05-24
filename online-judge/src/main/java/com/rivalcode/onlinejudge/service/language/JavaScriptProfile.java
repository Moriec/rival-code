package com.rivalcode.onlinejudge.service.language;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaScriptProfile implements LanguageProfile {

    @Override
    public ProgrammingLanguages language() {
        return ProgrammingLanguages.JAVASCRIPT;
    }

    @Override
    public String sourceFileName() {
        return "solution.js";
    }

    @Override
    public boolean requiresCompilation() {
        return false;
    }

    @Override
    public String[] compileCommand() {
        return new String[0];
    }

    @Override
    public String executableFileName() {
        return "solution.js";
    }

    @Override
    public List<String> executionCommand(long memoryLimitKb) {
        long heapMb = memoryLimitKb / 1024;
        return List.of("/usr/bin/node", "--max-old-space-size=" + heapMb, "solution.js");
    }

    @Override
    public boolean useIsolateMem() {
        return false;
    }

    @Override
    public int processLimit() {
        return 32;
    }

    @Override
    public List<String> extraDirMounts() {
        return List.of("--dir=/usr/bin", "--dir=/usr/lib");
    }
}
