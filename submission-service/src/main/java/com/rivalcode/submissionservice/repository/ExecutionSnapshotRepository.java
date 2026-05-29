package com.rivalcode.submissionservice.repository;

import com.rivalcode.submissionservice.model.ExecutionSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionSnapshotRepository extends JpaRepository<ExecutionSnapshotEntity, UUID> {
}
