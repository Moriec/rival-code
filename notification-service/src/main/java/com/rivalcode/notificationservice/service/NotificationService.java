package com.rivalcode.notificationservice.service;

import com.rivalcode.contracts.notifications.enums.NotificationStatus;
import com.rivalcode.contracts.notifications.model.MarkNotificationsReadRequest;
import com.rivalcode.contracts.notifications.model.NotificationCommand;
import com.rivalcode.contracts.notifications.model.NotificationDto;
import com.rivalcode.notificationservice.model.NotificationDeliveryAttemptEntity;
import com.rivalcode.notificationservice.model.NotificationEntity;
import com.rivalcode.notificationservice.model.ProcessedNotificationCommandEntity;
import com.rivalcode.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.rivalcode.notificationservice.repository.NotificationRepository;
import com.rivalcode.notificationservice.repository.ProcessedNotificationCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String SSE_CHANNEL = "SSE";

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    private final ProcessedNotificationCommandRepository processedCommandRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationCommandIdGenerator commandIdGenerator;
    private final SseNotificationHub sseNotificationHub;

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String userId, UUID currentUserId) {
        UUID requestedUserId = parseUserId(userId, currentUserId);
        ensureCurrentUser(requestedUserId, currentUserId);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(requestedUserId).stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Transactional
    public List<NotificationDto> markRead(MarkNotificationsReadRequest request, UUID currentUserId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is required");
        }
        UUID requestedUserId = parseUserId(request.getUserId(), currentUserId);
        ensureCurrentUser(requestedUserId, currentUserId);

        List<NotificationEntity> notifications;
        if (request.getNotificationIds() == null || request.getNotificationIds().isEmpty()) {
            notifications = notificationRepository.findByUserIdAndStatus(requestedUserId, NotificationStatus.NEW);
        } else {
            notifications = notificationRepository.findByUserIdAndNotificationIdIn(
                    requestedUserId,
                    request.getNotificationIds()
            );
        }

        Instant readAt = request.getReadAt() != null ? request.getReadAt() : Instant.now();
        notifications.stream()
                .filter(notification -> notification.getStatus() != NotificationStatus.ARCHIVED)
                .forEach(notification -> {
                    notification.setStatus(NotificationStatus.READ);
                    notification.setReadAt(readAt);
                });

        return notificationRepository.saveAll(notifications).stream()
                .sorted(Comparator.comparing(NotificationEntity::getCreatedAt).reversed())
                .map(notificationMapper::toDto)
                .toList();
    }

    public SseEmitter subscribe(UUID currentUserId) {
        return sseNotificationHub.subscribe(currentUserId);
    }

    @Transactional
    public Optional<NotificationDto> handleCommand(NotificationCommand command, CommandMetadata metadata) {
        validateCommand(command);

        String commandId = commandIdGenerator.commandId(command);
        if (processedCommandRepository.existsById(commandId)) {
            return Optional.empty();
        }

        UUID notificationId = command.getNotificationId() != null
                ? command.getNotificationId()
                : UUID.randomUUID();

        if (notificationRepository.existsById(notificationId)) {
            saveProcessedCommand(commandId, notificationId, metadata);
            return Optional.empty();
        }

        UUID userId = parseRequiredUuid(command.getUserId(), "userId");
        Instant createdAt = command.getCreatedAt() != null ? command.getCreatedAt() : Instant.now();
        NotificationEntity notification = NotificationEntity.builder()
                .notificationId(notificationId)
                .userId(userId)
                .type(command.getType())
                .status(NotificationStatus.NEW)
                .title(command.getTitle())
                .body(command.getBody())
                .payload(notificationMapper.payloadToJson(command.getPayload()))
                .createdAt(createdAt)
                .build();

        NotificationEntity saved = notificationRepository.save(notification);
        saveProcessedCommand(commandId, notificationId, metadata);
        return Optional.of(notificationMapper.toDto(saved));
    }

    @Transactional
    public void deliverLive(NotificationDto notification) {
        SseNotificationHub.DeliveryOutcome outcome = sseNotificationHub.emit(notification);
        deliveryAttemptRepository.save(NotificationDeliveryAttemptEntity.builder()
                .attemptId(UUID.randomUUID())
                .notificationId(notification.getNotificationId())
                .channel(SSE_CHANNEL)
                .status(outcome.status())
                .errorMessage(outcome.errorMessage())
                .attemptedAt(Instant.now())
                .build());
    }

    private void validateCommand(NotificationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("NotificationCommand is required");
        }
        parseRequiredUuid(command.getUserId(), "userId");
        if (command.getType() == null) {
            throw new IllegalArgumentException("NotificationCommand.type is required");
        }
        if (!StringUtils.hasText(command.getTitle())) {
            throw new IllegalArgumentException("NotificationCommand.title is required");
        }
    }

    private void saveProcessedCommand(String commandId, UUID notificationId, CommandMetadata metadata) {
        processedCommandRepository.save(ProcessedNotificationCommandEntity.builder()
                .commandId(commandId)
                .notificationId(notificationId)
                .sourceTopic(metadata != null ? metadata.topic() : null)
                .sourcePartition(metadata != null ? metadata.partition() : null)
                .sourceOffset(metadata != null ? metadata.offset() : null)
                .processedAt(Instant.now())
                .build());
    }

    private UUID parseUserId(String userId, UUID currentUserId) {
        if (!StringUtils.hasText(userId)) {
            return currentUserId;
        }
        return parseRequiredUuid(userId, "userId");
    }

    private UUID parseRequiredUuid(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a UUID", e);
        }
    }

    private void ensureCurrentUser(UUID requestedUserId, UUID currentUserId) {
        if (!requestedUserId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's notifications");
        }
    }
}
