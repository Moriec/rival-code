package com.rivalcode.notificationservice.kafka;

import com.rivalcode.contracts.notifications.model.NotificationCommand;
import com.rivalcode.notificationservice.service.CommandMetadata;
import com.rivalcode.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCommandConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.notification-commands-topic:notification-commands.v1}",
            groupId = "${app.kafka.notification-group-id:notification-service}",
            containerFactory = "notificationCommandKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, NotificationCommand> record, Acknowledgment acknowledgment) {
        NotificationCommand command = record.value();
        log.info("Received notification command for user {} from {}-{}@{}",
                command != null ? command.getUserId() : null,
                record.topic(),
                record.partition(),
                record.offset());

        notificationService.handleCommand(
                        command,
                        new CommandMetadata(record.topic(), record.partition(), record.offset()))
                .ifPresent(notificationService::deliverLive);

        acknowledgment.acknowledge();
    }
}
