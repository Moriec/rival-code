package com.rivalcode.problemservice.controller;

import com.rivalcode.problemservice.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final MinioService minioService;

    @PostMapping("/api/problems/{problemId}/versions/{versionId}/assets")
    @PreAuthorize("hasRole('ADMIN')")
    public String uploadAsset(@PathVariable UUID problemId, @PathVariable UUID versionId,
                              @RequestParam("file") MultipartFile file) {
        return minioService.uploadFileToAssets(file, problemId);
    }

    @PostMapping("/api/problems/{problemId}/versions/{versionId}/test-suite")
    @PreAuthorize("hasRole('ADMIN')")
    public String uploadTestSuite(@PathVariable UUID problemId, @PathVariable UUID versionId,
                                  @RequestParam("file") MultipartFile file) {
        return minioService.uploadTestArchive(file, problemId, versionId);
    }
}