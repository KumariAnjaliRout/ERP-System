package com.app.notification.service;

import com.app.notification.exception.SnsOperationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.notification.config.AwsSnsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;


import java.util.HashMap;
import java.util.Map;

//aws integration layer


@Slf4j
@Service
@RequiredArgsConstructor
public class SnsPushService {

    private final SnsClient snsClient;
    private final AwsSnsProperties properties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    private static final int MAX_ATTEMPTS = 3;

    // CREATE ENDPOINT
    public String createEndpoint(String deviceToken) {

        if (deviceToken == null || deviceToken.isBlank()) {
            throw new IllegalArgumentException("Device token cannot be empty");
        }

        if (properties.getPlatformArn() == null ||
                properties.getPlatformArn().isBlank()) {

            throw new IllegalStateException("SNS platformArn not configured");
        }

        try {

            CreatePlatformEndpointRequest request =
                    CreatePlatformEndpointRequest.builder()
                            .platformApplicationArn(properties.getPlatformArn())
                            .token(deviceToken)
                            .build();

            CreatePlatformEndpointResponse response =
                    snsClient.createPlatformEndpoint(request);

            String endpointArn = response.endpointArn();

            log.info("SNS endpoint created → {}", endpointArn);

            return endpointArn;

        } catch (Exception ex) {

            log.error("SNS endpoint creation failed → token={}", deviceToken, ex);

            throw new SnsOperationException("Failed to create SNS endpoint", ex);
        }
    }

    // SEND PUSH
    public boolean sendPush(String endpointArn,
                            String title,
                            String message,
                            String priority,
                            Map<String, String> data) {

        if (endpointArn == null || endpointArn.isBlank()) {
            throw new IllegalArgumentException("Endpoint ARN cannot be null");
        }

        String payload = buildPayload(title, message, priority, data);

        PublishRequest request = PublishRequest.builder()
                .targetArn(endpointArn)
                .messageStructure("json")
                .message(payload)
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {

                PublishResponse response = snsClient.publish(request);

                log.info("Push sent → endpoint={} messageId={}",
                        endpointArn,
                        response.messageId());

                safeMetric("push.success");
                return true;

            } catch (EndpointDisabledException ex) {

                log.warn("Push skipped → endpoint disabled → {}", endpointArn);

                safeMetric("push.disabled");
                return false;

            } catch (SnsException ex) {

                safeMetric("push.failure");

                if (attempt == MAX_ATTEMPTS) {
                    log.error("Push failed → endpoint={}", endpointArn, ex);
                    throw new SnsOperationException("Push delivery failed", ex);
                }

                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return false;
    }

    // BUILD PUSH PAYLOAD
    private String buildPayload(String title,
                                String message,
                                String priority,
                                Map<String, String> data) {

        try {

            Map<String, Object> aps = new HashMap<>();
            aps.put("alert", Map.of(
                    "title", title != null ? title : "",
                    "body", message != null ? message : ""
            ));

            if (priority != null) {
                aps.put("priority", priority);
            }

            Map<String, Object> apns = new HashMap<>();
            apns.put("aps", aps);

            Map<String, Object> payload = new HashMap<>();

            payload.put("default", message != null ? message : "");

            payload.put("APNS",
                    objectMapper.writeValueAsString(apns));

            payload.put("GCM",
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "notification",
                                    Map.of(
                                            "title", title != null ? title : "",
                                            "body", message != null ? message : ""
                                    ),
                                    "data",
                                    data != null ? data : Map.of()
                            )
                    )
            );

            return objectMapper.writeValueAsString(payload);

        } catch (Exception ex) {
            log.error("Failed to build SNS payload → title={} message={}", title, message, ex);
            throw new SnsOperationException("Failed to build SNS payload", ex);
        }
    }

    // SAFE METRICS
    private void safeMetric(String name) {

        try {
            meterRegistry.counter(name).increment();
        } catch (Exception ex) {
            log.debug("Metric failure ignored → {}", name);
        }
    }

    // ENDPOINT STATUS
    public boolean isEndpointEnabled(String endpointArn) {

        try {

            GetEndpointAttributesResponse response =
                    snsClient.getEndpointAttributes(
                            GetEndpointAttributesRequest.builder()
                                    .endpointArn(endpointArn)
                                    .build()
                    );

            return "true".equalsIgnoreCase(
                    response.attributes().get("Enabled")
            );

        } catch (Exception ex) {

            log.warn("Failed to fetch endpoint status → {}", endpointArn);

            return false;
        }
    }

    // DISABLE ENDPOINT
    public void disableEndpoint(String endpointArn) {

        try {

            snsClient.setEndpointAttributes(
                    SetEndpointAttributesRequest.builder()
                            .endpointArn(endpointArn)
                            .attributes(Map.of("Enabled", "false"))
                            .build()
            );

            log.info("SNS endpoint disabled → {}", endpointArn);

        } catch (Exception ex) {

            log.warn("Failed to disable SNS endpoint → {}", endpointArn, ex);
        }
    }

    // ENABLE ENDPOINT
    public void enableEndpoint(String endpointArn) {

        try {

            snsClient.setEndpointAttributes(
                    SetEndpointAttributesRequest.builder()
                            .endpointArn(endpointArn)
                            .attributes(Map.of("Enabled", "true"))
                            .build()
            );

            log.info("SNS endpoint enabled → {}", endpointArn);

        } catch (Exception ex) {

            log.warn("Failed to enable SNS endpoint → {}", endpointArn, ex);
        }
    }

    // DELETE ENDPOINT
    public void deleteEndpoint(String endpointArn) {

        try {

            snsClient.deleteEndpoint(
                    DeleteEndpointRequest.builder()
                            .endpointArn(endpointArn)
                            .build()
            );

            log.info("SNS endpoint deleted → {}", endpointArn);

        } catch (Exception ex) {
            log.warn("Failed to delete SNS endpoint → {}", endpointArn, ex);
        }
    }
}
















//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class SnsPushService {
//
//    private final SnsClient snsClient;
//    private final AwsSnsProperties properties;
//    private final MeterRegistry meterRegistry;
//    private final ObjectMapper objectMapper;
//
//    private static final int MAX_ATTEMPTS = 3;
//
//
//    public String createEndpoint(String deviceToken) {
//
//        if (deviceToken == null || deviceToken.isBlank()) {
//            throw new IllegalArgumentException("Device token cannot be empty");
//        }
//
//        try {
//
//            CreatePlatformEndpointRequest request =
//                    CreatePlatformEndpointRequest.builder()
//                            .platformApplicationArn(properties.getPlatformArn())
//                            .token(deviceToken)
//                            .build();
//
//            CreatePlatformEndpointResponse response =
//                    snsClient.createPlatformEndpoint(request);
//
//            log.info("SNS endpoint created → {}", response.endpointArn());
//
//            return response.endpointArn();
//
//        } catch (Exception ex) {
//
//            log.error("SNS endpoint creation failed → token={}", deviceToken, ex);
//
//            throw new SnsOperationException("Failed to create SNS endpoint", ex);
//        }
//    }
//    // CREATE ENDPOINT
////    public String createEndpoint(String deviceToken) {
////
////        if (deviceToken == null || deviceToken.isBlank()) {
////            throw new IllegalArgumentException("Device token cannot be empty");
////        }
////
////        log.info("Creating SNS endpoint for token={}", deviceToken);
////
////        try {
////
////            CreatePlatformEndpointRequest request =
////                    CreatePlatformEndpointRequest.builder()
////                            .platformApplicationArn(properties.getPlatformArn())
////                            .token(deviceToken)
////                            .build();
////
////            CreatePlatformEndpointResponse response =
////                    snsClient.createPlatformEndpoint(request);
////
////            log.info("SNS endpoint created successfully → arn={}",
////                    response.endpointArn());
////
////            return response.endpointArn();
////
////        } catch (Exception ex) {
////
////            log.error("Failed to create SNS endpoint → token={}",
////                    deviceToken, ex);
////
////            throw ex;
////        }
////    }
//
//    public void sendPush(String endpointArn,
//                         String title,
//                         String message,
//                         String priority,
//                         Map<String, String> data) {
//
//        if (endpointArn == null || endpointArn.isBlank()) {
//            throw new IllegalArgumentException("Endpoint ARN cannot be null");
//        }
//
//        String payload = buildPayload(title, message, priority, data);
//
//        PublishRequest request = PublishRequest.builder()
//                .targetArn(endpointArn)
//                .messageStructure("json")
//                .message(payload)
//                .build();
//
//        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
//
//            try {
//
//                PublishResponse response = snsClient.publish(request);
//
//                log.info("Push sent → endpoint={} messageId={}",
//                        endpointArn, response.messageId());
//
//                meterRegistry.counter("push.success").increment();
//                return;
//
//            } catch (EndpointDisabledException ex) {
//
//                log.warn("Push skipped → endpoint disabled → {}", endpointArn);
//
//                meterRegistry.counter("push.disabled").increment();
//                throw ex;
//
//            } catch (SnsException ex) {
//
//                meterRegistry.counter("push.failure").increment();
//
//                if (attempt == MAX_ATTEMPTS) {
//
//                    log.error("Push failed after retries → endpoint={}", endpointArn, ex);
//
//                    throw new SnsOperationException("Push delivery failed", ex);
//                }
//
//                try {
//                    Thread.sleep(500L * attempt);
//                } catch (InterruptedException ignored) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }
//    }

    // SEND PUSH
//    public void sendPush(String endpointArn,
//                         String title,
//                         String message,
//                         String priority,
//                         Map<String, String> data) {
//
//        if (endpointArn == null || endpointArn.isBlank()) {
//            throw new IllegalArgumentException("Endpoint ARN cannot be null");
//        }
//
//        String payload = buildPayload(title, message, priority, data);
//
//        PublishRequest request = PublishRequest.builder()
//                .targetArn(endpointArn)
//                .messageStructure("json")
//                .message(payload)
//                .build();
//
//        log.info("🚀 PUSH INIT → endpoint={} priority={} title={}",
//                endpointArn, priority, title);
//
//        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
//
//            try {
//
//                PublishResponse response = snsClient.publish(request);
//
//                log.info("✅ PUSH SUCCESS → endpoint={} messageId={} attempt={}",
//                        endpointArn,
//                        response.messageId(),
//                        attempt);
//
//                meterRegistry.counter("push.success").increment();
//                return;
//
//            } catch (EndpointDisabledException ex) {
//
//                log.warn("ENDPOINT DISABLED → endpoint={}", endpointArn);
//
//                meterRegistry.counter("push.disabled").increment();
//                throw ex;
//
//            } catch (SnsException ex) {
//
//                String errorMessage =
//                        ex.awsErrorDetails() != null
//                                ? ex.awsErrorDetails().errorMessage()
//                                : ex.getMessage();
//
//                log.warn("⚠ PUSH FAILED → endpoint={} attempt={} error={}",
//                        endpointArn,
//                        attempt,
//                        errorMessage);
//
//                meterRegistry.counter("push.failure").increment();
//
//                if (attempt == MAX_ATTEMPTS) {
//
//                    log.error("PUSH ABORTED AFTER MAX RETRIES → endpoint={} error={}",
//                            endpointArn,
//                            errorMessage);
//
//                    throw ex;
//                }
//
//                try {
//                    Thread.sleep(500L * attempt); // simple backoff
//                } catch (InterruptedException ignored) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }
//    }

    // ENDPOINT STATUS CHECK
//    public boolean isEndpointEnabled(String endpointArn) {
//
//        if (endpointArn == null || endpointArn.isBlank()) {
//            return false;
//        }
//
//        try {
//
//            GetEndpointAttributesResponse response =
//                    snsClient.getEndpointAttributes(
//                            GetEndpointAttributesRequest.builder()
//                                    .endpointArn(endpointArn)
//                                    .build()
//                    );
//
//            String enabled = response.attributes().get("Enabled");
//
//            boolean isEnabled = "true".equalsIgnoreCase(enabled);
//
//            log.info("Endpoint status check → endpoint={} enabled={}",
//                    endpointArn, isEnabled);
//
//            return isEnabled;
//
//        } catch (Exception ex) {
//
//            log.warn("Failed to fetch endpoint attributes → endpoint={}",
//                    endpointArn, ex);
//
//            return false;
//        }
//    }
//
//    // DELETE ENDPOINT
//    public void deleteEndpoint(String endpointArn) {
//
//        if (endpointArn == null || endpointArn.isBlank()) {
//            return;
//        }
//
//        try {
//
//            snsClient.deleteEndpoint(
//                    DeleteEndpointRequest.builder()
//                            .endpointArn(endpointArn)
//                            .build()
//            );
//
//            log.info("SNS endpoint deleted → {}", endpointArn);
//
//        } catch (Exception ex) {
//
//            log.error("Failed to delete SNS endpoint → {}",
//                    endpointArn, ex);
//        }
//    }
//
//    // BUILD FCM PAYLOAD (ANDROID SAFE)
//    private String buildPayload(String title,
//                                String message,
//                                String priority,
//                                Map<String, String> data) {
//
//        try {
//
//            Map<String, Object> notification = new HashMap<>();
//            notification.put("title", title);
//            notification.put("body", message);
//
//            Map<String, Object> androidNotification = new HashMap<>();
//            androidNotification.put("channel_id", "default");
//            androidNotification.put("sound", "default");
//
//            Map<String, Object> android = new HashMap<>();
//            android.put("priority",
//                    "HIGH".equalsIgnoreCase(priority) ? "high" : "normal");
//            android.put("notification", androidNotification);
//
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("notification", notification);
//            payload.put("android", android);
//
//            if (data != null && !data.isEmpty()) {
//                payload.put("data", data);
//            }
//
//            String gcmJson = objectMapper.writeValueAsString(payload);
//
//            return objectMapper.writeValueAsString(
//                    Map.of("GCM", gcmJson)
//            );
//
//        } catch (Exception ex) {
//
//            log.error("Push payload build failed → title={}", title, ex);
//
//            throw new RuntimeException("Push payload build failed", ex);
//        }
//    }
//}