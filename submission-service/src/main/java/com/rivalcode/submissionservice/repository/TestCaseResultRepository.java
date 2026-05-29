package com.rivalcode.submissionservice.repository;

import com.rivalcode.submissionservice.model.TestCaseResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseResultRepository extends JpaRepository<TestCaseResultEntity, UUID> {

    List<TestCaseResultEntity> findBySubmissionIdOrderByOrderNo(UUID submissionId);
}
