package com.rivalcode.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rivalcode.contracts.notifications.model.NotificationCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class NotificationCommandIdGenerator {

    private final ObjectMapper objectMapper;

    public NotificationCommandIdGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String commandId(NotificationCommand command) {
        if (command.getNotificationId() != null) {
            return command.getNotificationId().toString();
        }
        try {
            return sha256(objectMapper.writeValueAsString(command));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationCommand", e);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }
}
