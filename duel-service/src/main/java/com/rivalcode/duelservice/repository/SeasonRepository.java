package com.rivalcode.duelservice.repository;

import com.rivalcode.duelservice.model.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<SeasonEntity, UUID> {

    Optional<SeasonEntity> findFirstByActiveTrueOrderByStartsAtDesc();

    List<SeasonEntity> findAllByOrderByStartsAtDesc();
}
