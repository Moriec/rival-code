package com.rivalcode.onlinejudge.service.language;

import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaProfile implements LanguageProfile {

    private static final long JVM_ADDRESS_SPACE_OVERHEAD_KB = 1024L * 1024L;

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
        return new String[]{
                "/opt/java/openjdk/bin/javac",
                "-J-Xmx512m",
                "-J-Xss512k",
                "-J-XX:+UseSerialGC",
                "-J-XX:ActiveProcessorCount=2",
                "Main.java"
        };
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
                "-Xss512k",
                "-XX:+UseSerialGC",
                "-XX:ActiveProcessorCount=2",
                "-XX:ReservedCodeCacheSize=32m",
                "-XX:CompressedClassSpaceSize=16m",
                "-XX:MaxMetaspaceSize=64m",
                "-cp", "/box",
                "Main"
        );
    }

    @Override
    public long isolateMemoryLimitKb(long memoryLimitKb) {
        return memoryLimitKb + JVM_ADDRESS_SPACE_OVERHEAD_KB;
    }

    @Override
    public boolean useIsolateMem() {
        return true;
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
