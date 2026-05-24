package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;
import com.rivalcode.onlinejudge.service.Sandbox;
import com.rivalcode.onlinejudge.service.language.LanguageProfile;
import com.rivalcode.onlinejudge.service.language.LanguageProfileRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class IsolateSandbox implements Sandbox {

    private final LanguageProfileRegistry profileRegistry;

    @Value("${app.sandbox.default-wall-time-multiplier:2.0}")
    private double wallTimeMultiplier;

    @Override
    public CompilationResult compile(UserCode userCode) throws Exception {
        LanguageProfile profile = profileRegistry.getProfile(userCode.getLanguage());
        Path tempDir = Files.createTempDirectory("judge-compilation-");
        File sourceFile = new File(tempDir.toFile(), profile.sourceFileName());
        Files.writeString(sourceFile.toPath(), userCode.getSourceCode());

        if (!profile.requiresCompilation()) {
            return new CompilationResult(sourceFile, profile);
        }

        Process process = new ProcessBuilder(profile.compileCommand())
                .directory(tempDir.toFile())
                .start();

        if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("Compilation timed out");
        }

        if (process.exitValue() != 0) {
            String error = new String(process.getErrorStream().readAllBytes());
            throw new RuntimeException(error);
        }

        File executable = new File(tempDir.toFile(), profile.executableFileName());
        return new CompilationResult(executable, profile);
    }

    @Override
    public ExecutionResult run(int boxId, CompilationResult compilation, String input, Long timeLimitMs, Long memoryLimitKb) {
        Path metaFile = null;
        try {
            LanguageProfile profile = compilation.getProfile();
            String boxPath = initBox(boxId);
            prepareExecutableInBox(compilation.getExecutable(), boxPath, profile);
            metaFile = Files.createTempFile("isolate-meta-" + boxId + "-", ".txt");

            double timeLimitSec = timeLimitMs / 1000.0;

            List<String> command = new ArrayList<>(List.of(
                "isolate",
                "--box-id=" + boxId,
                "--time=" + timeLimitSec,
                "--wall-time=" + (timeLimitSec * wallTimeMultiplier),
                "--processes=" + profile.processLimit(),
                "--fsize=1024",
                "--chdir=/box",
                "--meta=" + metaFile.toAbsolutePath()
            ));

            for (String mount : profile.extraDirMounts()) {
                command.add(mount);
            }

            if (profile.useIsolateMem()) {
                command.add("--mem=" + memoryLimitKb);
            }

            command.add("--run");
            command.add("--");
            command.addAll(profile.executionCommand(memoryLimitKb));

            log.debug("Running isolate command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            if (input != null) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(input);
                    writer.flush();
                }
            }

            process.waitFor(timeLimitMs * 3, java.util.concurrent.TimeUnit.MILLISECONDS);

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            log.debug("Isolate stdout: {}", stdout);
            log.debug("Isolate stderr: {}", stderr);

            return parseMetaFile(metaFile, stdout, stderr);

        } catch (Exception e) {
            log.error("Error running isolate", e);
            return ExecutionResult.builder()
                    .status(JudgeStatus.SYSTEM_ERROR)
                    .stderr(e.getMessage())
                    .build();
        } finally {
            if (metaFile != null) {
                try { Files.deleteIfExists(metaFile); } catch (IOException ignored) {}
            }
        }
    }

    private String initBox(int boxId) throws IOException, InterruptedException {
        Process initProcess = new ProcessBuilder("isolate", "--box-id=" + boxId, "--init").start();
        if (!initProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            initProcess.destroyForcibly();
            throw new RuntimeException("isolate init timed out for box " + boxId);
        }
        if (initProcess.exitValue() != 0) {
            String stderr = new String(initProcess.getErrorStream().readAllBytes());
            throw new RuntimeException("isolate init failed for box " + boxId + ": " + stderr);
        }
        String boxPath = new String(initProcess.getInputStream().readAllBytes()).trim();
        if (boxPath.isBlank()) {
            throw new RuntimeException("isolate init returned empty box path for box " + boxId);
        }
        return boxPath;
    }

    private void prepareExecutableInBox(File executable, String boxPath, LanguageProfile profile) throws IOException {
        Path boxDir = Path.of(boxPath, "box");
        Path sourcePath = executable.toPath();
        Path targetPath = boxDir.resolve(executable.getName());
        Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        if (profile.requiresCompilation() && !executable.getName().endsWith(".class")) {
            Files.setPosixFilePermissions(targetPath, java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                    java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                    java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                    java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE,
                    java.nio.file.attribute.PosixFilePermission.OTHERS_READ,
                    java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
            ));
        } else {
            Files.setPosixFilePermissions(targetPath, java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                    java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                    java.nio.file.attribute.PosixFilePermission.OTHERS_READ
            ));
        }
    }

    ExecutionResult parseMetaFile(Path metaFile, String stdout, String stderr) throws IOException {
        Map<String, String> meta = new HashMap<>();
        try (var lines = Files.lines(metaFile)) {
            lines.forEach(line -> {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) meta.put(parts[0], parts[1]);
            });
        }

        log.debug("Isolate metadata: {}", meta);

        long timeMs = 0;
        long memoryKb = 0;
        int exitCode = 0;

        try {
            timeMs = Math.round(Double.parseDouble(meta.getOrDefault("time", "0")) * 1000);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse time from isolate metadata: {}", meta.get("time"), e);
        }

        try {
            String memStr = meta.containsKey("cg-mem") ? meta.get("cg-mem") : meta.getOrDefault("max-rss", "0");
            memoryKb = Long.parseLong(memStr);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse memory from isolate metadata", e);
        }

        try {
            exitCode = Integer.parseInt(meta.getOrDefault("exitcode", "0"));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse exitcode from isolate metadata: {}", meta.get("exitcode"), e);
        }

        String statusStr = meta.get("status");
        log.debug("Isolate status string: {}, exitCode: {}", statusStr, exitCode);

        JudgeStatus status = JudgeStatus.ACCEPTED;
        if (statusStr != null) {
            status = switch (statusStr) {
                case "TO" -> JudgeStatus.TIME_LIMIT_EXCEEDED;
                case "SG" -> {
                    String exitSig = meta.getOrDefault("exitsig", "");
                    if ("6".equals(exitSig) && stderr.contains("bad_alloc")) {
                        yield JudgeStatus.MEMORY_LIMIT_EXCEEDED;
                    }
                    yield JudgeStatus.RUNTIME_ERROR;
                }
                case "RE" -> JudgeStatus.RUNTIME_ERROR;
                case "MO" -> JudgeStatus.MEMORY_LIMIT_EXCEEDED;
                default -> JudgeStatus.RUNTIME_ERROR;
            };
        } else if (exitCode != 0) {
            status = JudgeStatus.RUNTIME_ERROR;
        } else if (!stderr.isBlank() && !stderr.stripLeading().startsWith("OK (")) {
            status = JudgeStatus.RUNTIME_ERROR;
        }

        log.debug("Determined status: {}", status);

        return ExecutionResult.builder()
                .stdout(stdout)
                .stderr(stderr)
                .exitCode(exitCode)
                .timeMs(timeMs)
                .memoryKb(memoryKb)
                .status(status)
                .build();
    }

    @Override
    public void cleanup(int boxId) {
        try {
            new ProcessBuilder("isolate", "--box-id=" + boxId, "--cleanup").start().waitFor();
        } catch (Exception e) {
            log.error("Cleanup failed for box {}", boxId, e);
        }
    }
}
