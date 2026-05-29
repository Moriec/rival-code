package com.rivalcode.notificationservice.repository;

import com.rivalcode.contracts.notifications.enums.NotificationStatus;
import com.rivalcode.notificationservice.model.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<NotificationEntity> findByUserIdAndNotificationIdIn(UUID userId, Collection<UUID> notificationIds);

    List<NotificationEntity> findByUserIdAndStatus(UUID userId, NotificationStatus status);
}
