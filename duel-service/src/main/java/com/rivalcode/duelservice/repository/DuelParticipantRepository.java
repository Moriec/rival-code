package com.rivalcode.duelservice.repository;

import com.rivalcode.duelservice.model.DuelParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DuelParticipantRepository extends JpaRepository<DuelParticipantEntity, UUID> {

    List<DuelParticipantEntity> findByDuelIdOrderByCreatedAtAsc(UUID duelId);

    List<DuelParticipantEntity> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<DuelParticipantEntity> findByDuelIdAndUserId(UUID duelId, UUID userId);
}
