package com.rivalcode.problemservice.repository;

import com.rivalcode.problemservice.model.ProblemExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProblemExampleRepository extends JpaRepository<ProblemExample, UUID> {

    List<ProblemExample> findByProblemVersionIdOrderByOrderNo(UUID versionId);

}