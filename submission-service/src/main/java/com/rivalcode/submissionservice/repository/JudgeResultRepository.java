package com.rivalcode.submissionservice.repository;

import com.rivalcode.submissionservice.model.JudgeResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JudgeResultRepository extends JpaRepository<JudgeResultEntity, UUID> {
}
