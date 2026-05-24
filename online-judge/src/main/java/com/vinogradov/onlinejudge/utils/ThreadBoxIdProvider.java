package com.vinogradov.onlinejudge.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns a unique, stable integer ID to each thread.
 * Useful for mapping Kafka consumer threads to Isolate box IDs.
 */
@Component
public class ThreadBoxIdProvider {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ThreadLocal<Integer> threadBoxId = ThreadLocal.withInitial(counter::getAndIncrement);

    public int getBoxId() {
        return threadBoxId.get();
    }
}
