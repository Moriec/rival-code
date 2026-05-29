package com.rivalcode.notificationservice.service;

import com.rivalcode.contracts.notifications.model.NotificationDto;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseNotificationHub {

    private static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("ready").comment("connected"));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    public DeliveryOutcome emit(NotificationDto notification) {
        UUID userId = UUID.fromString(notification.getUserId());
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return new DeliveryOutcome("SKIPPED", "No active SSE subscribers");
        }

        int sent = 0;
        int failed = 0;
        for (SseEmitter emitter : Set.copyOf(userEmitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(notification.getNotificationId().toString())
                        .data(notification));
                sent++;
            } catch (IOException | IllegalStateException e) {
                failed++;
                remove(userId, emitter);
            }
        }

        if (sent > 0) {
            String error = failed > 0 ? failed + " SSE emitter(s) failed" : null;
            return new DeliveryOutcome("SENT", error);
        }
        return new DeliveryOutcome("FAILED", failed + " SSE emitter(s) failed");
    }

    private void remove(UUID userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }

    public record DeliveryOutcome(String status, String errorMessage) {
    }
}
