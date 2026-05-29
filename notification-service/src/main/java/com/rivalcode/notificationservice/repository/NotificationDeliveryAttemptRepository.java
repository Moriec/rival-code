package com.rivalcode.notificationservice.repository;

import com.rivalcode.notificationservice.model.NotificationDeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {
}
