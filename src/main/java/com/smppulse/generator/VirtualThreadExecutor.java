package com.smppulse.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadExecutor {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadExecutor.class);

    private volatile ExecutorService executor;

    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Virtual thread executor started");
    }

    public void submit(Runnable task) {
        ExecutorService exec = executor;
        if (exec != null && !exec.isShutdown()) {
            exec.submit(task);
        }
    }

    public synchronized void shutdown(long timeoutSeconds) {
        if (executor == null || executor.isShutdown()) return;

        log.info("Shutting down virtual thread executor (timeout: {}s)", timeoutSeconds);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Virtual thread executor shut down");
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }
}
