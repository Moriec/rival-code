package com.rivalcode.duelservice.repository;

import com.rivalcode.contracts.duels.enums.DuelMode;
import com.rivalcode.duelservice.model.QueuePresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueuePresetRepository extends JpaRepository<QueuePresetEntity, UUID> {

    Optional<QueuePresetEntity> findFirstByModeAndActiveTrue(DuelMode mode);

    List<QueuePresetEntity> findAllByOrderByNameAsc();
}
