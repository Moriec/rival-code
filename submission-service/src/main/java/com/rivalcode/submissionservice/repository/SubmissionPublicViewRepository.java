package com.rivalcode.submissionservice.repository;

import com.rivalcode.submissionservice.model.SubmissionPublicViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubmissionPublicViewRepository extends JpaRepository<SubmissionPublicViewEntity, UUID> {
}
