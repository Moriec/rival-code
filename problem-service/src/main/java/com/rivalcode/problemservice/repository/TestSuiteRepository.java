package com.rivalcode.problemservice.repository;

import com.rivalcode.problemservice.model.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {

    Optional<TestSuite> findByProblemVersionId(UUID versionId);
}