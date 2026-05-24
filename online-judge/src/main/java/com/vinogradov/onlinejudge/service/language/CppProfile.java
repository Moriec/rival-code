package com.vinogradov.onlinejudge.service.language;

import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CppProfile implements LanguageProfile {

    @Override
    public ProgrammingLanguages language() {
        return ProgrammingLanguages.CPP;
    }

    @Override
    public String sourceFileName() {
        return "solution.cpp";
    }

    @Override
    public boolean requiresCompilation() {
        return true;
    }

    @Override
    public String[] compileCommand() {
        return new String[]{"g++", "-O3", "solution.cpp", "-o", "solution"};
    }

    @Override
    public String executableFileName() {
        return "solution";
    }

    @Override
    public List<String> executionCommand(long memoryLimitKb) {
        return List.of("./solution");
    }

    @Override
    public boolean useIsolateMem() {
        return true;
    }

    @Override
    public int processLimit() {
        return 16;
    }

    @Override
    public List<String> extraDirMounts() {
        return List.of();
    }
}
