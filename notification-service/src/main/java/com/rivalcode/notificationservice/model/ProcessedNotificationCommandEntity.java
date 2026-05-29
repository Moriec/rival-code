package com.rivalcode.notificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_notification_commands", schema = "notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedNotificationCommandEntity {

    @Id
    @Column(name = "command_id", length = 256)
    private String commandId;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "source_topic", length = 128)
    private String sourceTopic;

    @Column(name = "source_partition")
    private Integer sourcePartition;

    @Column(name = "source_offset")
    private Long sourceOffset;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
