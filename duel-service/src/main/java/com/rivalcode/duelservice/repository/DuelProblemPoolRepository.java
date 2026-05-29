package com.rivalcode.duelservice.repository;

import com.rivalcode.duelservice.model.DuelProblemPoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DuelProblemPoolRepository extends JpaRepository<DuelProblemPoolEntity, UUID> {

    Optional<DuelProblemPoolEntity> findFirstBySeasonIdAndPresetIdAndActiveTrue(UUID seasonId, UUID presetId);

    Optional<DuelProblemPoolEntity> findFirstByPresetIdAndActiveTrue(UUID presetId);

    List<DuelProblemPoolEntity> findAllByOrderByNameAsc();
}
