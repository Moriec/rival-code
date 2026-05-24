package com.rivalcode.contracts.notifications.model;

import com.rivalcode.contracts.notifications.enums.NotificationType;
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
public class NotificationCommand {
    private UUID notificationId;
    private String userId;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, Object> payload;
    private Instant createdAt;
}
