package com.rivalcode.notificationservice.service;

public record CommandMetadata(
        String topic,
        Integer partition,
        Long offset
) {
}
