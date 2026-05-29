package com.rivalcode.duelservice.repository;

import com.rivalcode.duelservice.model.DuelEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DuelEventLogRepository extends JpaRepository<DuelEventLogEntity, UUID> {
}
