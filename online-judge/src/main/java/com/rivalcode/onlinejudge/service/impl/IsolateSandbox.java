package com.rivalcode.onlinejudge.service.impl;

import com.rivalcode.contracts.submissionResult.enums.JudgeStatus;
import com.rivalcode.contracts.submissions.enums.ProgrammingLanguages;
import com.rivalcode.contracts.submissions.model.UserCode;
import com.rivalcode.onlinejudge.exception.CompilationFailedException;
import com.rivalcode.onlinejudge.model.CheckResult;
import com.rivalcode.onlinejudge.model.CompilationResult;
import com.rivalcode.onlinejudge.model.ExecutionResult;
import com.rivalcode.onlinejudge.service.Sandbox;
import com.rivalcode.onlinejudge.service.language.LanguageProfile;
import com.rivalcode.onlinejudge.service.language.LanguageProfileRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class IsolateSandbox implements Sandbox {

    private static final String BOX_DIR_NAME = "box";
    private static final String SANDBOX_PATH = "/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";
    private static final List<String> COMMON_DIR_MOUNTS = List.of(
            "--dir=/usr",
            "--dir=/usr/local:maybe",
            "--dir=/lib:maybe",
            "--dir=/lib64:maybe",
            "--dir=/tmp:tmp"
    );

    private final LanguageProfileRegistry profileRegistry;

    @Value("${app.sandbox.default-wall-time-multiplier:2.0}")
    private double wallTimeMultiplier;

    @Value("${app.sandbox.compile-time-limit-ms:30000}")
    private long compileTimeLimitMs;

    @Value("${app.sandbox.compile-memory-limit-kb:4194304}")
    private long compileMemoryLimitKb;

    @Value("${app.sandbox.compile-output-limit-bytes:67108864}")
    private long compileOutputLimitBytes;

    @Value("${app.sandbox.custom-checker-time-limit-ms:2000}")
    private long customCheckerTimeLimitMs;

    @Value("${app.sandbox.custom-checker-memory-limit-kb:65536}")
    private long customCheckerMemoryLimitKb;

    @Value("${app.sandbox.custom-checker-output-limit-bytes:65536}")
    private long customCheckerOutputLimitBytes;

    @Override
    public CompilationResult compile(int boxId, UserCode userCode) throws Exception {
        LanguageProfile profile = profileRegistry.getProfile(userCode.getLanguage());
        Path workspace = Files.createTempDirectory("judge-compilation-");
        Path sourceDirectory = Files.createDirectories(workspace.resolve("source"));
        Path artifactsDirectory = Files.createDirectories(workspace.resolve("artifacts"));
        Path sourceFile = sourceDirectory.resolve(profile.sourceFileName());
        Files.writeString(sourceFile, userCode.getSourceCode(), StandardCharsets.UTF_8);

        try {
            if (!profile.requiresCompilation()) {
                Files.copy(sourceFile, artifactsDirectory.resolve(sourceFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                return new CompilationResult(workspace, artifactsDirectory, profile);
            }

            compileInIsolate(boxId, profile, sourceFile, artifactsDirectory);
            return new CompilationResult(workspace, artifactsDirectory, profile);
        } catch (Exception e) {
            deleteDirectory(workspace);
            throw e;
        }
    }

    @Override
    public ExecutionResult run(int boxId, CompilationResult compilation, String input, Long timeLimitMs, Long memoryLimitKb, Long outputLimitBytes) {
        Path metaFile = null;
        try {
            LanguageProfile profile = compilation.getProfile();
            String boxPath = initBox(boxId);
            prepareArtifactsInBox(compilation, boxPath);
            metaFile = Files.createTempFile("isolate-meta-" + boxId + "-", ".txt");

            List<String> command = buildIsolateCommand(
                    boxId,
                    timeLimitMs,
                    profile.isolateMemoryLimitKb(memoryLimitKb),
                    outputLimitBytes,
                    profile.processLimit(),
                    metaFile,
                    profile.extraDirMounts(),
                    profile.useIsolateMem()
            );
            command.add("--run");
            command.add("--");
            command.addAll(profile.executionCommand(memoryLimitKb));

            log.debug("Running isolate command: {}", String.join(" ", command));

            ProcessResult processResult = runProcess(command, null, input, watchdogTimeout(timeLimitMs));
            if (processResult.timedOut()) {
                return ExecutionResult.builder()
                        .status(JudgeStatus.TIME_LIMIT_EXCEEDED)
                        .stdout(processResult.stdout())
                        .stderr(processResult.stderr())
                        .message("Execution watchdog timed out")
                        .build();
            }

            return parseMetaFile(metaFile, processResult.stdout(), processResult.stderr());
        } catch (Exception e) {
            log.error("Error running isolate", e);
            return ExecutionResult.builder()
                    .status(JudgeStatus.SYSTEM_ERROR)
                    .stderr(e.getMessage())
                    .message(e.getMessage())
                    .build();
        } finally {
            deleteFile(metaFile);
            cleanup(boxId);
        }
    }

    @Override
    public CheckResult runPythonChecker(int boxId, String checkerCode, String input, String expectedOutput, String actualOutput) {
        Path workspace = null;
        Path metaFile = null;
        try {
            workspace = Files.createTempDirectory("judge-checker-");
            Path checkerFile = workspace.resolve("checker.py");
            Path inputFile = workspace.resolve("input.txt");
            Path expectedFile = workspace.resolve("expected.txt");
            Path actualFile = workspace.resolve("actual.txt");
            Files.writeString(checkerFile, checkerCode, StandardCharsets.UTF_8);
            Files.writeString(inputFile, input != null ? input : "", StandardCharsets.UTF_8);
            Files.writeString(expectedFile, expectedOutput != null ? expectedOutput : "", StandardCharsets.UTF_8);
            Files.writeString(actualFile, actualOutput != null ? actualOutput : "", StandardCharsets.UTF_8);

            String boxPath = initBox(boxId);
            Path boxDirectory = Path.of(boxPath, BOX_DIR_NAME);
            copyFileToBox(checkerFile, boxDirectory.resolve("checker.py"), false);
            copyFileToBox(inputFile, boxDirectory.resolve("input.txt"), false);
            copyFileToBox(expectedFile, boxDirectory.resolve("expected.txt"), false);
            copyFileToBox(actualFile, boxDirectory.resolve("actual.txt"), false);

            metaFile = Files.createTempFile("isolate-checker-meta-" + boxId + "-", ".txt");
            List<String> command = buildIsolateCommand(
                    boxId,
                    customCheckerTimeLimitMs,
                    customCheckerMemoryLimitKb,
                    customCheckerOutputLimitBytes,
                    16,
                    metaFile,
                    List.of("--dir=/usr/bin", "--dir=/usr/lib"),
                    true
            );
            command.add("--run");
            command.add("--");
            command.addAll(List.of("/usr/bin/python3", "checker.py", "input.txt", "expected.txt", "actual.txt"));

            ProcessResult processResult = runProcess(command, null, null, watchdogTimeout(customCheckerTimeLimitMs));
            if (processResult.timedOut()) {
                return checkerSystemError("Custom checker watchdog timed out", processResult.stderr());
            }

            return interpretPythonCheckerResult(parseMetaFile(metaFile, processResult.stdout(), processResult.stderr()));
        } catch (Exception e) {
            log.error("Error running custom checker", e);
            return checkerSystemError("Custom checker failed: " + e.getMessage(), null);
        } finally {
            deleteFile(metaFile);
            cleanup(boxId);
            deleteDirectory(workspace);
        }
    }

    CheckResult interpretPythonCheckerResult(ExecutionResult checkerResult) {
        if (checkerResult == null) {
            return checkerSystemError("Custom checker returned no result", null);
        }
        if (checkerResult.getStatus() == JudgeStatus.TIME_LIMIT_EXCEEDED) {
            return checkerSystemError("Custom checker timed out", checkerResult.getStderr());
        }
        if (checkerResult.getStatus() == JudgeStatus.MEMORY_LIMIT_EXCEEDED) {
            return checkerSystemError("Custom checker exceeded memory limit", checkerResult.getStderr());
        }
        if (checkerResult.getStatus() == JudgeStatus.OUTPUT_LIMIT_EXCEEDED) {
            return checkerSystemError("Custom checker exceeded output limit", checkerResult.getStderr());
        }

        int exitCode = checkerResult.getExitCode();
        Integer stdoutVerdict = parseStdoutVerdict(checkerResult.getStdout());
        if (stdoutVerdict != null) {
            if (stdoutVerdict == 1 && exitCode != 0) {
                return checkerSystemError(
                        "Custom checker printed ACCEPTED but exited with code " + exitCode,
                        firstNonBlank(checkerResult.getStderr(), checkerResult.getStdout(), checkerResult.getMessage())
                );
            }
            return stdoutVerdict == 1
                    ? CheckResult.builder().status(JudgeStatus.ACCEPTED).build()
                    : CheckResult.builder()
                    .status(JudgeStatus.WRONG_ANSWER)
                    .message(firstNonBlank(checkerResult.getStderr(), "Custom checker rejected output"))
                    .build();
        }

        if (checkerResult.getStdout() != null && !checkerResult.getStdout().isBlank() && exitCode == 0) {
            return checkerSystemError("Custom checker produced unsupported stdout verdict", checkerResult.getStdout());
        }
        if (checkerResult.getStatus() != JudgeStatus.ACCEPTED && exitCode != 1) {
            return checkerSystemError(
                    "Custom checker runtime error",
                    firstNonBlank(checkerResult.getStderr(), checkerResult.getMessage())
            );
        }
        if (exitCode == 0) {
            return CheckResult.builder().status(JudgeStatus.ACCEPTED).build();
        }
        if (exitCode == 1) {
            if (looksLikePythonCheckerRuntimeError(checkerResult.getStderr())) {
                return checkerSystemError("Custom checker runtime error", checkerResult.getStderr());
            }
            return CheckResult.builder()
                    .status(JudgeStatus.WRONG_ANSWER)
                    .message(firstNonBlank(checkerResult.getStderr(), "Custom checker rejected output"))
                    .build();
        }

        return checkerSystemError(
                "Custom checker failed with exit code " + exitCode,
                firstNonBlank(checkerResult.getStderr(), checkerResult.getStdout(), checkerResult.getMessage())
        );
    }

    private Integer parseStdoutVerdict(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            return null;
        }

        String[] lines = stdout.strip().split("\\R");
        for (int index = lines.length - 1; index >= 0; index--) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }
            String token = line.split("\\s+", 2)[0];
            if ("1".equals(token)) {
                return 1;
            }
            if ("0".equals(token)) {
                return 0;
            }
            return null;
        }
        return null;
    }

    private void compileInIsolate(int boxId, LanguageProfile profile, Path sourceFile, Path artifactsDirectory) throws Exception {
        Path metaFile = null;
        try {
            String boxPath = initBox(boxId);
            Path boxDirectory = Path.of(boxPath, BOX_DIR_NAME);
            copyFileToBox(sourceFile, boxDirectory.resolve(sourceFile.getFileName()), false);
            metaFile = Files.createTempFile("isolate-compile-meta-" + boxId + "-", ".txt");

            List<String> command = buildIsolateCommand(
                    boxId,
                    compileTimeLimitMs,
                    compileMemoryLimitKb,
                    compileOutputLimitBytes,
                    profile.processLimit(),
                    metaFile,
                    profile.extraDirMounts(),
                    true
            );
            command.add("--run");
            command.add("--");
            command.addAll(List.of(profile.compileCommand()));

            ProcessResult processResult = runProcess(command, null, null, watchdogTimeout(compileTimeLimitMs));
            if (processResult.timedOut()) {
                throw new CompilationFailedException("Compilation watchdog timed out");
            }

            ExecutionResult compilationRun = parseMetaFile(metaFile, processResult.stdout(), processResult.stderr());
            if (compilationRun.getStatus() == JudgeStatus.TIME_LIMIT_EXCEEDED) {
                throw new CompilationFailedException("Compilation timed out");
            }
            if (compilationRun.getStatus() == JudgeStatus.MEMORY_LIMIT_EXCEEDED) {
                throw new CompilationFailedException("Compilation exceeded memory limit");
            }
            if (compilationRun.getExitCode() != 0 || compilationRun.getStatus() != JudgeStatus.ACCEPTED) {
                throw new CompilationFailedException(firstNonBlank(compilationRun.getStderr(), compilationRun.getStdout(), compilationRun.getMessage(), "Compilation failed"));
            }

            copyRuntimeArtifactsFromBox(profile, boxDirectory, artifactsDirectory);
        } finally {
            deleteFile(metaFile);
            cleanup(boxId);
        }
    }

    private List<String> buildIsolateCommand(
            int boxId,
            long timeLimitMs,
            long memoryLimitKb,
            long outputLimitBytes,
            int processLimit,
            Path metaFile,
            List<String> extraDirMounts,
            boolean useMemoryLimit
    ) {
        double timeLimitSec = timeLimitMs / 1000.0;
        List<String> command = new ArrayList<>(List.of(
                "isolate",
                "--box-id=" + boxId,
                "--time=" + timeLimitSec,
                "--wall-time=" + (timeLimitSec * wallTimeMultiplier),
                "--processes=" + processLimit,
                "--fsize=" + toIsolateFileSizeKb(outputLimitBytes),
                "--chdir=/box",
                "--meta=" + metaFile.toAbsolutePath(),
                "--env=PATH=" + SANDBOX_PATH,
                "--env=HOME=/tmp"
        ));

        command.addAll(COMMON_DIR_MOUNTS);
        if (extraDirMounts != null) {
            command.addAll(extraDirMounts);
        }
        if (useMemoryLimit) {
            command.add("--mem=" + memoryLimitKb);
        }
        return command;
    }

    private long toIsolateFileSizeKb(long outputLimitBytes) {
        return Math.max(1, (outputLimitBytes + 1023) / 1024);
    }

    private Duration watchdogTimeout(long timeLimitMs) {
        long wallTimeMs = Math.round(timeLimitMs * wallTimeMultiplier);
        return Duration.ofMillis(Math.max(timeLimitMs + 5000, wallTimeMs + 5000));
    }

    private String initBox(int boxId) throws IOException, InterruptedException {
        ProcessResult result = runProcess(List.of("isolate", "--box-id=" + boxId, "--init"), null, null, Duration.ofSeconds(10));
        if (result.timedOut()) {
            throw new RuntimeException("isolate init timed out for box " + boxId);
        }
        if (result.exitCode() != 0) {
            throw new RuntimeException("isolate init failed for box " + boxId + ": " + result.stderr());
        }
        String boxPath = result.stdout().trim();
        if (boxPath.isBlank()) {
            throw new RuntimeException("isolate init returned empty box path for box " + boxId);
        }
        return boxPath;
    }

    private void prepareArtifactsInBox(CompilationResult compilation, String boxPath) throws IOException {
        Path boxDirectory = Path.of(boxPath, BOX_DIR_NAME);
        LanguageProfile profile = compilation.getProfile();
        boolean executableArtifacts = profile.requiresCompilation() && profile.language() != ProgrammingLanguages.JAVA;

        try (var paths = Files.walk(compilation.getArtifactsDirectory())) {
            for (Path artifact : paths.filter(Files::isRegularFile).toList()) {
                Path relative = compilation.getArtifactsDirectory().relativize(artifact);
                copyFileToBox(artifact, boxDirectory.resolve(relative), executableArtifacts);
            }
        }
    }

    void copyRuntimeArtifactsFromBox(LanguageProfile profile, Path boxDirectory, Path artifactsDirectory) throws IOException, CompilationFailedException {
        if (profile.language() == ProgrammingLanguages.JAVA) {
            try (var paths = Files.list(boxDirectory)) {
                List<Path> classFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .toList();
                if (classFiles.isEmpty()) {
                    throw new CompilationFailedException("Compilation produced no Java class files");
                }
                for (Path classFile : classFiles) {
                    Files.copy(classFile, artifactsDirectory.resolve(classFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return;
        }

        Path executable = boxDirectory.resolve(profile.executableFileName());
        if (!Files.isRegularFile(executable)) {
            throw new CompilationFailedException("Compilation produced no executable artifact: " + profile.executableFileName());
        }
        Files.copy(executable, artifactsDirectory.resolve(executable.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyFileToBox(Path source, Path target, boolean executable) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        Set<PosixFilePermission> permissions = executable
                ? Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
        )
                : Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ
        );
        Files.setPosixFilePermissions(target, permissions);
    }

    ExecutionResult parseMetaFile(Path metaFile, String stdout, String stderr) throws IOException {
        Map<String, String> meta = new HashMap<>();
        try (var lines = Files.lines(metaFile)) {
            lines.forEach(line -> {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    meta.put(parts[0], parts[1]);
                }
            });
        }

        log.debug("Isolate metadata: {}", meta);

        long timeMs = parseTimeMs(meta);
        long memoryKb = parseMemoryKb(meta);
        int exitCode = parseExitCode(meta);
        String statusStr = meta.get("status");
        String message = meta.get("message");

        JudgeStatus status = JudgeStatus.ACCEPTED;
        if (isOutputLimit(statusStr, message, stderr)) {
            status = JudgeStatus.OUTPUT_LIMIT_EXCEEDED;
        } else if (statusStr != null) {
            status = switch (statusStr) {
                case "TO" -> JudgeStatus.TIME_LIMIT_EXCEEDED;
                case "SG" -> {
                    String exitSig = meta.getOrDefault("exitsig", "");
                    if ("6".equals(exitSig) && stderr != null && stderr.contains("bad_alloc")) {
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
        }

        log.debug("Determined status: {}", status);

        return ExecutionResult.builder()
                .stdout(stdout)
                .stderr(stderr)
                .exitCode(exitCode)
                .timeMs(timeMs)
                .memoryKb(memoryKb)
                .status(status)
                .message(firstNonBlank(message, status == JudgeStatus.ACCEPTED ? null : stderr, null))
                .build();
    }

    private boolean isOutputLimit(String statusStr, String message, String stderr) {
        if ("FO".equals(statusStr) || "OL".equals(statusStr)) {
            return true;
        }
        String text = firstNonBlank(message, stderr);
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("file size limit") || lower.contains("output limit");
    }

    private long parseTimeMs(Map<String, String> meta) {
        try {
            return Math.round(Double.parseDouble(meta.getOrDefault("time", "0")) * 1000);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse time from isolate metadata: {}", meta.get("time"), e);
            return 0;
        }
    }

    private long parseMemoryKb(Map<String, String> meta) {
        try {
            String memStr = meta.containsKey("cg-mem") ? meta.get("cg-mem") : meta.getOrDefault("max-rss", "0");
            return Long.parseLong(memStr);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse memory from isolate metadata", e);
            return 0;
        }
    }

    private int parseExitCode(Map<String, String> meta) {
        try {
            return Integer.parseInt(meta.getOrDefault("exitcode", "0"));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse exitcode from isolate metadata: {}", meta.get("exitcode"), e);
            return 0;
        }
    }

    private ProcessResult runProcess(List<String> command, Path directory, String input, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (directory != null) {
            processBuilder.directory(directory.toFile());
        }

        Process process = processBuilder.start();
        CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
        CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

        try {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                if (input != null) {
                    writer.write(input);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            log.debug("Process stdin closed before input was fully written", e);
        }

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            boolean stopped = process.waitFor(5, TimeUnit.SECONDS);
            return new ProcessResult(
                    stopped ? process.exitValue() : -1,
                    getFutureValue(stdoutFuture),
                    getFutureValue(stderrFuture),
                    true
            );
        }

        return new ProcessResult(
                process.exitValue(),
                getFutureValue(stdoutFuture),
                getFutureValue(stderrFuture),
                false
        );
    }

    private CompletableFuture<String> readStreamAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private String getFutureValue(CompletableFuture<String> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return "";
        } catch (Exception e) {
            log.warn("Failed to read process stream", e);
            return "";
        }
    }

    private CheckResult checkerSystemError(String message, String details) {
        return CheckResult.builder()
                .status(JudgeStatus.SYSTEM_ERROR)
                .message(firstNonBlank(details, message, "Custom checker failed"))
                .build();
    }

    private boolean looksLikePythonCheckerRuntimeError(String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return false;
        }
        String lower = stderr.toLowerCase();
        return lower.contains("traceback")
                || lower.contains("syntaxerror")
                || lower.contains("nameerror")
                || lower.contains("typeerror")
                || lower.contains("valueerror")
                || lower.contains("runtimeerror");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void cleanup(int boxId) {
        try {
            ProcessResult result = runProcess(List.of("isolate", "--box-id=" + boxId, "--cleanup"), null, null, Duration.ofSeconds(10));
            if (result.exitCode() != 0 && !result.stderr().isBlank()) {
                log.warn("Cleanup for box {} returned non-zero exit code {}: {}", boxId, result.exitCode(), result.stderr());
            }
        } catch (Exception e) {
            log.error("Cleanup failed for box {}", boxId, e);
        }
    }

    private void deleteFile(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete {}", file, e);
        }
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to delete directory {}", directory, e);
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {
    }
}
