package com.rivalcode.onlinejudge.service.language;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PythonProfile implements LanguageProfile {

    @Override
    public ProgrammingLanguages language() {
        return ProgrammingLanguages.PYTHON;
    }

    @Override
    public String sourceFileName() {
        return "solution.py";
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
        return "solution.py";
    }

    @Override
    public List<String> executionCommand(long memoryLimitKb) {
        return List.of("/usr/bin/python3", "solution.py");
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
        return List.of("--dir=/usr/bin", "--dir=/usr/lib");
    }
}
