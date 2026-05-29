package com.rivalcode.duelservice.repository;

import com.rivalcode.duelservice.model.RatingHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RatingHistoryRepository extends JpaRepository<RatingHistoryEntity, UUID> {

    List<RatingHistoryEntity> findTop20ByUserIdOrderByChangedAtDesc(UUID userId);
}
