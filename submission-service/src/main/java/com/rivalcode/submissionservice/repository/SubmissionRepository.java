package com.rivalcode.submissionservice.repository;

import com.rivalcode.submissionservice.model.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    List<SubmissionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
