package com.app.notification.sse;

import com.app.notification.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

//Send events to active emitters

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSsePublisher {

    private final SseEmitterRegistry emitterRegistry;
    public void publishNotification(UUID userId,
                                    NotificationResponseDto dto) {

        sendEvent(userId, "notification", dto);
    }

    public void publishUnreadCount(UUID userId,
                                   long unreadCount) {

        log.debug("Unread count update → user={} count={}", userId, unreadCount);
        sendEvent(userId, "unread-count", unreadCount);
    }

    private void sendEvent(UUID userId,
                           String eventName,
                           Object data) {

        if (userId == null || data == null) {
            log.debug("Skipping SSE event → user={} event={}", userId, eventName);
            return;
        }

        List<SseEmitter> emitters = emitterRegistry.getEmitters(userId);
        if (emitters.isEmpty()) {
            log.debug("No active SSE connections → user={}", userId);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data)
                );
            } catch (IOException ex) {

                // Client disconnected
                log.warn("SSE connection closed → user={}", userId);

                safeComplete(emitter);
                emitterRegistry.remove(userId, emitter);

            } catch (IllegalStateException ex) {

                // Emitter already closed
                log.warn("SSE already completed → user={}", userId);

                emitterRegistry.remove(userId, emitter);

            } catch (Exception ex) {

                // Unexpected runtime issue
                log.error("Unexpected SSE error → user={}", userId, ex);

                safeCompleteWithError(emitter, ex);
                emitterRegistry.remove(userId, emitter);
            }
        }
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

