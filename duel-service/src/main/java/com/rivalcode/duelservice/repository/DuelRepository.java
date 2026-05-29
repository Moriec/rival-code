package com.rivalcode.duelservice.repository;

import com.rivalcode.contracts.duels.enums.DuelStatus;
import com.rivalcode.duelservice.model.DuelEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DuelRepository extends JpaRepository<DuelEntity, UUID> {

    List<DuelEntity> findTop50ByStatusAndEndsAtBeforeOrderByEndsAtAsc(DuelStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DuelEntity> findByDuelId(UUID duelId);
}
