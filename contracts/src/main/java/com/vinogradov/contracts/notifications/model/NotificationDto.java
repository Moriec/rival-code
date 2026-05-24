package com.vinogradov.contracts.notifications.model;

import com.vinogradov.contracts.notifications.enums.NotificationStatus;
import com.vinogradov.contracts.notifications.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID notificationId;
    private String userId;
    private NotificationType type;
    private NotificationStatus status;
    private String title;
    private String body;
    private Map<String, Object> payload;
    private Instant createdAt;
    private Instant readAt;
}
