package com.rivalcode.onlinejudge.utils;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThreadBoxIdProviderTest {

    @Test
    void shouldReturnSameIdForSameThread() {
        ThreadBoxIdProvider provider = new ThreadBoxIdProvider();
        int id1 = provider.getBoxId();
        int id2 = provider.getBoxId();
        
        assertEquals(id1, id2, "One thread should always get the same Box ID");
    }

    @Test
    void shouldReturnDifferentIdsForDifferentThreads() throws InterruptedException {
        ThreadBoxIdProvider provider = new ThreadBoxIdProvider();
        Set<Integer> ids = Collections.synchronizedSet(new HashSet<>());
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ids.add(provider.getBoxId());
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        assertEquals(threadCount, ids.size(), "Each thread should have a unique Box ID");
    }
}
