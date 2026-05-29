package com.rivalcode.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rivalcode.contracts.notifications.enums.NotificationType;
import com.rivalcode.contracts.notifications.model.NotificationCommand;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCommandIdGeneratorTest {

    private final NotificationCommandIdGenerator generator =
            new NotificationCommandIdGenerator(new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void usesNotificationIdWhenPresent() {
        UUID notificationId = UUID.randomUUID();
        NotificationCommand command = NotificationCommand.builder()
                .notificationId(notificationId)
                .userId(UUID.randomUUID().toString())
                .type(NotificationType.SYSTEM)
                .title("Title")
                .createdAt(Instant.now())
                .build();

        assertThat(generator.commandId(command)).isEqualTo(notificationId.toString());
    }

    @Test
    void hashesCommandWithoutNotificationIdStableAcrossPayloadOrder() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-26T10:00:00Z");
        NotificationCommand first = NotificationCommand.builder()
                .userId(userId.toString())
                .type(NotificationType.MATCH_FOUND)
                .title("Match found")
                .payload(new LinkedHashMap<>(Map.of("duelId", "d1", "opponentId", "u2")))
                .createdAt(createdAt)
                .build();
        LinkedHashMap<String, Object> reversedPayload = new LinkedHashMap<>();
        reversedPayload.put("opponentId", "u2");
        reversedPayload.put("duelId", "d1");
        NotificationCommand second = NotificationCommand.builder()
                .userId(userId.toString())
                .type(NotificationType.MATCH_FOUND)
                .title("Match found")
                .payload(reversedPayload)
                .createdAt(createdAt)
                .build();

        assertThat(generator.commandId(first)).isEqualTo(generator.commandId(second));
    }
}
