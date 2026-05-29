package com.rivalcode.notificationservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.notifications.model.NotificationDto;
import com.rivalcode.notificationservice.model.NotificationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ObjectMapper objectMapper;

    public NotificationDto toDto(NotificationEntity entity) {
        return NotificationDto.builder()
                .notificationId(entity.getNotificationId())
                .userId(entity.getUserId().toString())
                .type(entity.getType())
                .status(entity.getStatus())
                .title(entity.getTitle())
                .body(entity.getBody())
                .payload(toMap(entity.getPayload()))
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }

    public JsonNode payloadToJson(Map<String, Object> payload) {
        return payload != null ? objectMapper.valueToTree(payload) : null;
    }

    private Map<String, Object> toMap(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        return objectMapper.convertValue(payload, new TypeReference<>() {
        });
    }
}
