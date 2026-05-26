package com.rivalcode.problemservice.repository;

import com.rivalcode.problemservice.model.ProblemVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemVersionRepository extends JpaRepository<ProblemVersion, UUID> {

    Optional<ProblemVersion> findByProblemIdAndActiveTrue(UUID problemId);

    List<ProblemVersion> findByProblemIdOrderByVersionNumberDesc(UUID problemId);

    Optional<ProblemVersion> findTopByProblemIdOrderByVersionNumberDesc(UUID problemId);
}