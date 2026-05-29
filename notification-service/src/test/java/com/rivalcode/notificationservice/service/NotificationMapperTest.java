package com.rivalcode.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.notifications.enums.NotificationStatus;
import com.rivalcode.contracts.notifications.enums.NotificationType;
import com.rivalcode.contracts.notifications.model.NotificationDto;
import com.rivalcode.notificationservice.model.NotificationEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper(new ObjectMapper());

    @Test
    void mapsPayloadWithoutLosingFields() {
        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.RATING_CHANGED)
                .status(NotificationStatus.NEW)
                .title("Rating changed")
                .body("+15")
                .payload(mapper.payloadToJson(Map.of("oldRating", 1200, "newRating", 1215)))
                .createdAt(Instant.now())
                .build();

        NotificationDto dto = mapper.toDto(entity);

        assertThat(dto.getPayload())
                .containsEntry("oldRating", 1200)
                .containsEntry("newRating", 1215);
    }
}
