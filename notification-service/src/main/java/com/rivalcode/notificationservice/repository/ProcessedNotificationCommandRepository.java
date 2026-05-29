package com.rivalcode.notificationservice.repository;

import com.rivalcode.notificationservice.model.ProcessedNotificationCommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedNotificationCommandRepository
        extends JpaRepository<ProcessedNotificationCommandEntity, String> {
}
