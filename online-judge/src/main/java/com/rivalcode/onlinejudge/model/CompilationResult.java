package com.rivalcode.onlinejudge.model;

import com.rivalcode.onlinejudge.service.language.LanguageProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Getter
@AllArgsConstructor
@Slf4j
public class CompilationResult implements AutoCloseable {
    private final Path workspaceDirectory;
    private final Path artifactsDirectory;
    private final LanguageProfile profile;

    @Override
    public void close() {
        if (workspaceDirectory == null) {
            return;
        }
        try (var paths = Files.walk(workspaceDirectory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete compilation artifact {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to clean compilation workspace {}", workspaceDirectory, e);
        }
    }
}
