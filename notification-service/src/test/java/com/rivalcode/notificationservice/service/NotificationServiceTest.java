package com.rivalcode.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivalcode.contracts.notifications.enums.NotificationStatus;
import com.rivalcode.contracts.notifications.enums.NotificationType;
import com.rivalcode.contracts.notifications.model.MarkNotificationsReadRequest;
import com.rivalcode.contracts.notifications.model.NotificationCommand;
import com.rivalcode.contracts.notifications.model.NotificationDto;
import com.rivalcode.notificationservice.model.NotificationEntity;
import com.rivalcode.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.rivalcode.notificationservice.repository.NotificationRepository;
import com.rivalcode.notificationservice.repository.ProcessedNotificationCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    @Mock
    private ProcessedNotificationCommandRepository processedCommandRepository;
    @Mock
    private SseNotificationHub sseNotificationHub;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        NotificationMapper mapper = new NotificationMapper(objectMapper);
        NotificationCommandIdGenerator commandIdGenerator = new NotificationCommandIdGenerator(objectMapper);
        notificationService = new NotificationService(
                notificationRepository,
                deliveryAttemptRepository,
                processedCommandRepository,
                mapper,
                commandIdGenerator,
                sseNotificationHub
        );
    }

    @Test
    void commandCreatesNewNotification() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-26T10:00:00Z");
        NotificationCommand command = NotificationCommand.builder()
                .notificationId(notificationId)
                .userId(userId.toString())
                .type(NotificationType.SYSTEM)
                .title("System")
                .body("Hello")
                .createdAt(createdAt)
                .build();

        when(processedCommandRepository.existsById(notificationId.toString())).thenReturn(false);
        when(notificationRepository.existsById(notificationId)).thenReturn(false);
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<NotificationDto> result = notificationService.handleCommand(
                command,
                new CommandMetadata("notification-commands.v1", 0, 7L)
        );

        assertThat(result).isPresent();
        assertThat(result.get().getNotificationId()).isEqualTo(notificationId);
        assertThat(result.get().getStatus()).isEqualTo(NotificationStatus.NEW);
        assertThat(result.get().getCreatedAt()).isEqualTo(createdAt);
        verify(processedCommandRepository).save(argThat(processed ->
                processed.getCommandId().equals(notificationId.toString())
                        && processed.getSourceOffset().equals(7L)));
    }

    @Test
    void duplicateCommandDoesNotCreateSecondNotification() {
        UUID notificationId = UUID.randomUUID();
        NotificationCommand command = NotificationCommand.builder()
                .notificationId(notificationId)
                .userId(UUID.randomUUID().toString())
                .type(NotificationType.SYSTEM)
                .title("System")
                .build();
        when(processedCommandRepository.existsById(notificationId.toString())).thenReturn(true);

        Optional<NotificationDto> result = notificationService.handleCommand(command, null);

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markReadRejectsAnotherUser() {
        UUID currentUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        MarkNotificationsReadRequest request = MarkNotificationsReadRequest.builder()
                .userId(anotherUserId.toString())
                .notificationIds(List.of(UUID.randomUUID()))
                .build();

        assertThatThrownBy(() -> notificationService.markRead(request, currentUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void markReadChangesOnlySelectedCurrentUserNotifications() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationEntity notification = NotificationEntity.builder()
                .notificationId(notificationId)
                .userId(userId)
                .type(NotificationType.PRACTICE_JUDGED)
                .status(NotificationStatus.NEW)
                .title("Judged")
                .createdAt(Instant.now())
                .build();
        when(notificationRepository.findByUserIdAndNotificationIdIn(userId, List.of(notificationId)))
                .thenReturn(List.of(notification));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<NotificationDto> result = notificationService.markRead(
                MarkNotificationsReadRequest.builder()
                        .userId(userId.toString())
                        .notificationIds(List.of(notificationId))
                        .build(),
                userId
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(result.getFirst().getReadAt()).isNotNull();
    }
}
