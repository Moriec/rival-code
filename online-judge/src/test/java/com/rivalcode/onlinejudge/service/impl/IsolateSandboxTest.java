package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.onlinejudge.model.ExecutionResult;
import com.rivalcode.onlinejudge.service.language.JavaProfile;
import com.rivalcode.onlinejudge.service.language.LanguageProfileRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IsolateSandboxTest {

    private final IsolateSandbox sandbox = new IsolateSandbox(new LanguageProfileRegistry(List.of()));

    @Test
    void shouldParseAcceptedMetaFile(@TempDir Path tempDir) throws Exception {
        Path metaFile = tempDir.resolve("meta.txt");
        Files.writeString(metaFile, """
                time:0.050
                time-wall:0.060
                max-rss:15420
                exitcode:0
                """);

        ExecutionResult result = sandbox.parseMetaFile(metaFile, "output", "");

        assertEquals(JudgeStatus.ACCEPTED, result.getStatus());
        assertEquals(50L, result.getTimeMs());
        assertEquals(15420L, result.getMemoryKb());
        assertEquals("output", result.getStdout());
    }

    @Test
    void shouldParseTimeLimitExceededMetaFile(@TempDir Path tempDir) throws Exception {
        Path metaFile = tempDir.resolve("meta.txt");
        Files.writeString(metaFile, """
                time:1.001
                status:TO
                message:Time limit exceeded
                """);

        ExecutionResult result = sandbox.parseMetaFile(metaFile, "", "");

        assertEquals(JudgeStatus.TIME_LIMIT_EXCEEDED, result.getStatus());
        assertEquals(1001L, result.getTimeMs());
    }

    @Test
    void shouldParseMemoryLimitExceededMetaFile(@TempDir Path tempDir) throws Exception {
        Path metaFile = tempDir.resolve("meta.txt");
        Files.writeString(metaFile, """
                time:0.200
                max-rss:256000
                status:MO
                """);

        ExecutionResult result = sandbox.parseMetaFile(metaFile, "", "");

        assertEquals(JudgeStatus.MEMORY_LIMIT_EXCEEDED, result.getStatus());
        assertEquals(256000L, result.getMemoryKb());
    }

    @Test
    void shouldNotTreatStderrAsRuntimeErrorWhenExitCodeIsZero(@TempDir Path tempDir) throws Exception {
        Path metaFile = tempDir.resolve("meta.txt");
        Files.writeString(metaFile, """
                time:0.050
                exitcode:0
                """);

        ExecutionResult result = sandbox.parseMetaFile(metaFile, "ok", "debug log");

        assertEquals(JudgeStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void shouldParseOutputLimitExceededMetaFile(@TempDir Path tempDir) throws Exception {
        Path metaFile = tempDir.resolve("meta.txt");
        Files.writeString(metaFile, """
                time:0.100
                status:FO
                message:File size limit exceeded
                """);

        ExecutionResult result = sandbox.parseMetaFile(metaFile, "", "");

        assertEquals(JudgeStatus.OUTPUT_LIMIT_EXCEEDED, result.getStatus());
    }

    @Test
    void shouldInterpretPythonCheckerStdoutOneAsAccepted() {
        ExecutionResult checkerResult = ExecutionResult.builder()
                .status(JudgeStatus.ACCEPTED)
                .exitCode(0)
                .stdout("1\n")
                .build();

        assertEquals(JudgeStatus.ACCEPTED, sandbox.interpretPythonCheckerResult(checkerResult).getStatus());
    }

    @Test
    void shouldInterpretPythonCheckerStdoutZeroAsWrongAnswer() {
        ExecutionResult checkerResult = ExecutionResult.builder()
                .status(JudgeStatus.ACCEPTED)
                .exitCode(0)
                .stdout("0\n")
                .build();

        assertEquals(JudgeStatus.WRONG_ANSWER, sandbox.interpretPythonCheckerResult(checkerResult).getStatus());
    }

    @Test
    void shouldUseLastPythonCheckerOutputLineAsVerdict() {
        ExecutionResult checkerResult = ExecutionResult.builder()
                .status(JudgeStatus.ACCEPTED)
                .exitCode(0)
                .stdout("debug\n1\n")
                .build();

        assertEquals(JudgeStatus.ACCEPTED, sandbox.interpretPythonCheckerResult(checkerResult).getStatus());
    }

    @Test
    void shouldKeepLegacyPythonCheckerExitCodeFallbackWhenStdoutIsBlank() {
        ExecutionResult checkerResult = ExecutionResult.builder()
                .status(JudgeStatus.RUNTIME_ERROR)
                .exitCode(1)
                .stdout("")
                .build();

        assertEquals(JudgeStatus.WRONG_ANSWER, sandbox.interpretPythonCheckerResult(checkerResult).getStatus());
    }

    @Test
    void shouldTreatUnsupportedPythonCheckerStdoutAsSystemError() {
        ExecutionResult checkerResult = ExecutionResult.builder()
                .status(JudgeStatus.ACCEPTED)
                .exitCode(0)
                .stdout("accepted\n")
                .build();

        assertEquals(JudgeStatus.SYSTEM_ERROR, sandbox.interpretPythonCheckerResult(checkerResult).getStatus());
    }

    @Test
    void shouldCopyAllJavaClassArtifacts(@TempDir Path tempDir) throws Exception {
        Path boxDir = Files.createDirectories(tempDir.resolve("box"));
        Path artifactsDir = Files.createDirectories(tempDir.resolve("artifacts"));
        Files.writeString(boxDir.resolve("Main.class"), "main");
        Files.writeString(boxDir.resolve("Main$FastScanner.class"), "nested");
        Files.writeString(boxDir.resolve("Pair.class"), "helper");
        Files.writeString(boxDir.resolve("Main.java"), "source");

        sandbox.copyRuntimeArtifactsFromBox(new JavaProfile(), boxDir, artifactsDir);

        assertTrue(Files.exists(artifactsDir.resolve("Main.class")));
        assertTrue(Files.exists(artifactsDir.resolve("Main$FastScanner.class")));
        assertTrue(Files.exists(artifactsDir.resolve("Pair.class")));
        assertFalse(Files.exists(artifactsDir.resolve("Main.java")));
    }
}
