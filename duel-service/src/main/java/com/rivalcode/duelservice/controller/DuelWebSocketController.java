package com.rivalcode.duelservice.controller;

import com.rivalcode.contracts.duels.model.CodeSnapshotMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class DuelWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/duels/{duelId}/code")
    public void codeSnapshot(@DestinationVariable String duelId, CodeSnapshotMessage message) {
        if (message == null || !StringUtils.hasText(message.getUserId())) {
            return;
        }
        message.setDuelId(duelId);
        if (message.getSentAt() == null) {
            message.setSentAt(Instant.now());
        }
        messagingTemplate.convertAndSend(
                "/topic/duels/" + duelId + "/code",
                message
        );
        messagingTemplate.convertAndSend(
                "/topic/duels/" + duelId + "/code/" + message.getUserId(),
                message
        );
    }

    @MessageMapping("/duels/{duelId}/presence")
    public void presence(@DestinationVariable String duelId) {
        messagingTemplate.convertAndSend("/topic/duels/" + duelId + "/presence", Instant.now());
    }
}
