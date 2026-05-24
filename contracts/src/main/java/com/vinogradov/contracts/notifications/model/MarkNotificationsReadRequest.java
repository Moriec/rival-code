package com.vinogradov.contracts.notifications.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkNotificationsReadRequest {
    private String userId;
    private List<UUID> notificationIds;
    private Instant readAt;
}
