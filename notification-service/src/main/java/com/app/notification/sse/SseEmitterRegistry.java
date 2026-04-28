package com.app.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter addEmitter(UUID userId) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters
                .computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        log.info("SSE connected → user={} totalConnections={}",
                userId,
                emitters.get(userId).size());

        emitter.onCompletion(() -> {
            log.debug("SSE completed → user={}", userId);
            remove(userId, emitter);
        });

        emitter.onTimeout(() -> {

            log.warn("SSE timeout → user={}", userId);

            safeComplete(emitter);
            remove(userId, emitter);
        });

        emitter.onError(error -> {

            log.warn("SSE error → user={} reason={}",
                    userId,
                    error != null ? error.getMessage() : "unknown");

            remove(userId, emitter);
        });

        return emitter;
    }
    public List<SseEmitter> getEmitters(UUID userId) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        return list != null ? list : Collections.emptyList();
    }

    public void remove(UUID userId, SseEmitter emitter) {

        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);

        if (list == null) {
            return;
        }

        list.remove(emitter);

        if (list.isEmpty()) {

            emitters.remove(userId);

            log.debug("All SSE connections removed → user={}", userId);
        }
    }

    // Heartbeat every 15 seconds
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {

        emitters.forEach((userId, emitterList) -> {

            for (SseEmitter emitter : emitterList) {
                if (emitter == null) {
                    continue;
                }
                try {

                    emitter.send(
                            SseEmitter.event()
                                    .name("heartbeat")
                                    .data("ping")
                    );

                } catch (IOException ex) {

                    log.warn("Heartbeat failed (client closed) → user={}", userId);

                    safeComplete(emitter);
                    remove(userId, emitter);

                } catch (IllegalStateException ex) {

                    log.debug("Heartbeat skipped (already closed) → user={}", userId);

                    remove(userId, emitter);

                } catch (Exception ex) {

                    log.error("Heartbeat unexpected error → user={}", userId, ex);

                    safeCompleteWithError(emitter, ex);
                    remove(userId, emitter);
                }
            }
        });
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {}
    }

    private void safeCompleteWithError(SseEmitter emitter, Exception ex) {
        try {
            emitter.completeWithError(ex);
        } catch (Exception ignored) {}
    }
}
