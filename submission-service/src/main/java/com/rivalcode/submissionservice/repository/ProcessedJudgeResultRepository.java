package com.rivalcode.submissionservice.repository;

import com.rivalcode.submissionservice.model.ProcessedJudgeResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedJudgeResultRepository extends JpaRepository<ProcessedJudgeResultEntity, UUID> {
}
