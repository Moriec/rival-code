package com.rivalcode.problemservice.repository;

import com.rivalcode.problemservice.model.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findBySlugIgnoreCase(String slug);

    @Query("""
    SELECT p FROM Problem p
    WHERE (:status IS NULL OR p.status = :status)
      AND (:difficulty IS NULL OR p.difficulty = :difficulty)
      AND (COALESCE(:search, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))
""")
    Page<Problem> findByFilters(@Param("status") String status,
                                @Param("difficulty") String difficulty,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("SELECT p FROM Problem p JOIN ProblemTag pt ON p.problemId = pt.id.problemId " +
            "WHERE pt.id.tagId IN :tagIds " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:difficulty IS NULL OR p.difficulty = :difficulty)")
    Page<Problem> findByTagsAndFilters(@Param("tagIds") List<UUID> tagIds,
                                       @Param("status") String status,
                                       @Param("difficulty") String difficulty,
                                       Pageable pageable);
}