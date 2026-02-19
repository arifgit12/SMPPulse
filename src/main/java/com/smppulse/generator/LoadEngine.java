package com.smppulse.generator;

import com.smppulse.config.DlrConfig;
import com.smppulse.config.LoadTestConfig;
import com.smppulse.config.MessageTemplate;
import com.smppulse.core.session.SessionManager;
import com.smppulse.core.session.SmppSessionWrapper;
import com.smppulse.dlr.DlrTracker;
import com.smppulse.metrics.MetricsCollector;
import com.smppulse.param.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LoadEngine {

    private static final Logger log = LoggerFactory.getLogger(LoadEngine.class);

    private final SessionManager sessionManager;
    private final ParameterResolver parameterResolver;
    private final MetricsCollector metricsCollector;
    private final DlrTracker dlrTracker;
    private final VirtualThreadExecutor executor = new VirtualThreadExecutor();
    private final AtomicReference<LoadTestState> state = new AtomicReference<>(LoadTestState.IDLE);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final List<Consumer<LoadTestState>> stateListeners = new CopyOnWriteArrayList<>();

    private volatile RateLimiter rateLimiter;
    private volatile Thread generatorThread;
    private volatile LoadTestConfig currentConfig;
    private volatile MessageTemplate currentTemplate;
    private volatile DlrConfig currentDlrConfig;
    private volatile long startTimeMs;

    public LoadEngine(SessionManager sessionManager, ParameterResolver parameterResolver,
                      MetricsCollector metricsCollector, DlrTracker dlrTracker) {
        this.sessionManager = sessionManager;
        this.parameterResolver = parameterResolver;
        this.metricsCollector = metricsCollector;
        this.dlrTracker = dlrTracker;
    }

    public void addStateListener(Consumer<LoadTestState> listener) {
        stateListeners.add(listener);
    }

    public void start(LoadTestConfig config, MessageTemplate template, DlrConfig dlrConfig) {
        if (!state.compareAndSet(LoadTestState.IDLE, LoadTestState.RUNNING) &&
            !state.compareAndSet(LoadTestState.STOPPED, LoadTestState.RUNNING)) {
            log.warn("Cannot start load test in state: {}", state.get());
            return;
        }

        if (!sessionManager.hasConnectedSession()) {
            setState(LoadTestState.IDLE);
            throw new IllegalStateException("No connected SMPP sessions");
        }

        this.currentConfig = config;
        this.currentTemplate = template;
        this.currentDlrConfig = dlrConfig;
        this.messagesSent.set(0);
        this.startTimeMs = System.currentTimeMillis();

        metricsCollector.reset();
        metricsCollector.markStart();
        parameterResolver.resetSequence();

        rateLimiter = new RateLimiter(config.getTargetTps());

        if (config.getRampUpSeconds() > 0) {
            rateLimiter.configureRamp(1, config.getTargetTps(), config.getRampUpSeconds() * 1000L);
        }

        executor.start();

        generatorThread = Thread.ofVirtual().name("load-generator").start(this::generateLoop);

        log.info("Load test started: targetTps={}, duration={}s, totalMessages={}",
                config.getTargetTps(), config.getDurationSeconds(), config.getTotalMessages());
        notifyStateChange();
    }

    private void generateLoop() {
        try {
            while (state.get() == LoadTestState.RUNNING) {
                // Check completion criteria
                if (isTestComplete()) {
                    break;
                }

                // Check for pause
                if (state.get() == LoadTestState.PAUSED) {
                    Thread.sleep(100);
                    continue;
                }

                // Rate limiting
                rateLimiter.acquire();

                if (state.get() != LoadTestState.RUNNING) break;

                // Handle ramp-down
                if (currentConfig.getRampDownSeconds() > 0 && shouldRampDown()) {
                    long remaining = getRemainingMs();
                    if (remaining > 0 && remaining <= currentConfig.getRampDownSeconds() * 1000L) {
                        rateLimiter.configureRamp(
                                rateLimiter.getCurrentRate(), 1,
                                remaining
                        );
                    }
                }

                // Get session and submit
                SmppSessionWrapper session = sessionManager.getNextSession();
                if (session == null || !session.isConnected()) {
                    Thread.sleep(100);
                    continue;
                }

                MessageSubmitter submitter = new MessageSubmitter(
                        session, parameterResolver, metricsCollector, dlrTracker,
                        currentTemplate, currentConfig, currentDlrConfig
                );

                executor.submit(submitter);
                messagesSent.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Load generator interrupted");
        } catch (Exception e) {
            log.error("Error in load generator", e);
        } finally {
            setState(LoadTestState.STOPPING);
            metricsCollector.markEnd();
            executor.shutdown(10);
            setState(LoadTestState.STOPPED);
            log.info("Load test completed. Messages sent: {}", messagesSent.get());
        }
    }

    private boolean isTestComplete() {
        if (currentConfig.getTotalMessages() > 0) {
            return messagesSent.get() >= currentConfig.getTotalMessages();
        }
        if (currentConfig.getDurationSeconds() > 0) {
            long elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000;
            return elapsedSeconds >= currentConfig.getDurationSeconds();
        }
        return false;
    }

    private boolean shouldRampDown() {
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        long totalMs = currentConfig.getDurationSeconds() * 1000L;
        long rampDownMs = currentConfig.getRampDownSeconds() * 1000L;
        return elapsedMs >= (totalMs - rampDownMs);
    }

    private long getRemainingMs() {
        long totalMs = currentConfig.getDurationSeconds() * 1000L;
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        return totalMs - elapsedMs;
    }

    public void pause() {
        if (state.compareAndSet(LoadTestState.RUNNING, LoadTestState.PAUSED)) {
            log.info("Load test paused");
            notifyStateChange();
        }
    }

    public void resume() {
        if (state.compareAndSet(LoadTestState.PAUSED, LoadTestState.RUNNING)) {
            log.info("Load test resumed");
            notifyStateChange();
        }
    }

    public void stop() {
        LoadTestState current = state.get();
        if (current == LoadTestState.IDLE || current == LoadTestState.STOPPED ||
            current == LoadTestState.STOPPING) {
            return;
        }

        setState(LoadTestState.STOPPING);
        if (generatorThread != null) {
            generatorThread.interrupt();
        }
    }

    public LoadTestProgress getProgress() {
        long elapsed = startTimeMs > 0 ? (System.currentTimeMillis() - startTimeMs) / 1000 : 0;
        return new LoadTestProgress(
                state.get(),
                messagesSent.get(),
                currentConfig != null ? currentConfig.getTotalMessages() : 0,
                elapsed,
                currentConfig != null ? currentConfig.getDurationSeconds() : 0,
                rateLimiter != null ? rateLimiter.getCurrentRate() : 0
        );
    }

    private void setState(LoadTestState newState) {
        state.set(newState);
        notifyStateChange();
    }

    private void notifyStateChange() {
        LoadTestState current = state.get();
        for (Consumer<LoadTestState> listener : stateListeners) {
            listener.accept(current);
        }
    }

    public LoadTestState getState() { return state.get(); }
    public long getMessagesSent() { return messagesSent.get(); }
}
