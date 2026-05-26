package com.rivalcode.onlinejudge.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CompilationResultTest {

    @Test
    void shouldDeleteWorkspaceOnClose(@TempDir Path tempDir) throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        Path artifacts = Files.createDirectories(workspace.resolve("artifacts"));
        Files.writeString(artifacts.resolve("Main.class"), "bytes");

        CompilationResult result = new CompilationResult(workspace, artifacts, null);
        result.close();

        assertFalse(Files.exists(workspace));
    }
}
