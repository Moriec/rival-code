package com.rivalcode.onlinejudge.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns a unique, stable integer ID to each thread.
 * Useful for mapping Kafka consumer threads to Isolate box IDs.
 */
@Component
public class ThreadBoxIdProvider {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Value("${app.sandbox.max-boxes:100}")
    private int maxBoxes = 100;

    private final ThreadLocal<Integer> threadBoxId = ThreadLocal.withInitial(this::nextBoxId);

    public int getBoxId() {
        return threadBoxId.get();
    }

    private int nextBoxId() {
        int boxId = counter.getAndIncrement();
        if (boxId >= maxBoxes) {
            throw new IllegalStateException("No isolate boxes available. maxBoxes=" + maxBoxes);
        }
        return boxId;
    }
}
