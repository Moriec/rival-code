package com.vinogradov.onlinejudge.service.language;

import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaProfile implements LanguageProfile {

    @Override
    public ProgrammingLanguages language() {
        return ProgrammingLanguages.JAVA;
    }

    @Override
    public String sourceFileName() {
        return "Main.java";
    }

    @Override
    public boolean requiresCompilation() {
        return true;
    }

    @Override
    public String[] compileCommand() {
        return new String[]{"javac", "Main.java"};
    }

    @Override
    public String executableFileName() {
        return "Main.class";
    }

    @Override
    public List<String> executionCommand(long memoryLimitKb) {
        long heapMb = memoryLimitKb / 1024;
        return List.of(
                "/opt/java/openjdk/bin/java",
                "-Xmx" + heapMb + "m",
                "-XX:CompressedClassSpaceSize=64m",
                "-XX:MaxMetaspaceSize=64m",
                "-cp", "/box",
                "Main"
        );
    }

    @Override
    public boolean useIsolateMem() {
        return false;
    }

    @Override
    public int processLimit() {
        return 64;
    }

    @Override
    public List<String> extraDirMounts() {
        return List.of("--dir=/opt/java");
    }
}
