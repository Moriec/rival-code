package com.rivalcode.duelservice.repository;

import com.rivalcode.duelservice.model.DuelProblemPoolItemEntity;
import com.rivalcode.duelservice.model.DuelProblemPoolItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DuelProblemPoolItemRepository extends JpaRepository<DuelProblemPoolItemEntity, DuelProblemPoolItemId> {

    List<DuelProblemPoolItemEntity> findById_PoolId(UUID poolId);

    void deleteById_PoolId(UUID poolId);
}
