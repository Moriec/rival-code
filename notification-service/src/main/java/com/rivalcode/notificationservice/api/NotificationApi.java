package com.rivalcode.notificationservice.api;

import com.rivalcode.contracts.notifications.model.MarkNotificationsReadRequest;
import com.rivalcode.contracts.notifications.model.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Inbox notifications and live stream")
public interface NotificationApi {

    @GetMapping
    @Operation(summary = "Get user inbox notifications")
    List<NotificationDto> getNotifications(@RequestParam(required = false) String userId);

    @PostMapping("/read")
    @Operation(summary = "Mark notifications as read")
    List<NotificationDto> markRead(@Valid @RequestBody MarkNotificationsReadRequest request);

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live notification stream")
    SseEmitter stream();
}
