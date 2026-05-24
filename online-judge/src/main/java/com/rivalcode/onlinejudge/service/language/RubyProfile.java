package com.rivalcode.onlinejudge.service.language;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RubyProfile implements LanguageProfile {

    @Override
    public ProgrammingLanguages language() {
        return ProgrammingLanguages.RUBY;
    }

    @Override
    public String sourceFileName() {
        return "solution.rb";
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
        return "solution.rb";
    }

    @Override
    public List<String> executionCommand(long memoryLimitKb) {
        return List.of("/usr/bin/ruby", "solution.rb");
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
