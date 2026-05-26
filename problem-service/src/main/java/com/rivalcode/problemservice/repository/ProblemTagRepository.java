package com.rivalcode.problemservice.repository;

import com.rivalcode.problemservice.model.ProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProblemTagRepository extends JpaRepository<ProblemTag, ProblemTag.ProblemTagId> {

    List<ProblemTag> findByProblem_ProblemId(UUID problemId);
}