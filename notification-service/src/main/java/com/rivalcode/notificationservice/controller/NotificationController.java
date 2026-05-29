package com.rivalcode.notificationservice.controller;

import com.rivalcode.contracts.notifications.model.MarkNotificationsReadRequest;
import com.rivalcode.contracts.notifications.model.NotificationDto;
import com.rivalcode.notificationservice.api.NotificationApi;
import com.rivalcode.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    public List<NotificationDto> getNotifications(String userId) {
        return notificationService.getNotifications(userId, getCurrentUserId());
    }

    @Override
    public List<NotificationDto> markRead(MarkNotificationsReadRequest request) {
        return notificationService.markRead(request, getCurrentUserId());
    }

    @Override
    public SseEmitter stream() {
        return notificationService.subscribe(getCurrentUserId());
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return UUID.fromString(userId);
    }
}
