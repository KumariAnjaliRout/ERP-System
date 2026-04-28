package com.app.notification.exception;

import com.app.notification.domain.ApiErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(NotificationRoutingException.class)
    public ResponseEntity<?> handleRouting(
            NotificationRoutingException ex,
            HttpServletRequest request) {

        log.error("Notification routing error", ex);

        return buildResponse(HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request);
    }
    @ExceptionHandler(NotificationQueryException.class)
    public ResponseEntity<?> handleQuery(
            NotificationQueryException ex,
            HttpServletRequest request) {

        log.error("Notification query error", ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(NotificationUpdateException.class)
    public ResponseEntity<?> handleUpdate(
            NotificationUpdateException ex,
            HttpServletRequest request) {

        log.error("Notification update error", ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<?> handleExternalService(
            ExternalServiceException ex,
            HttpServletRequest request) {

        log.error("External service error", ex);

        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "External service unavailable",
                request);
    }

    @ExceptionHandler(SecurityContextException.class)
    public ResponseEntity<?> handleSecurity(
            SecurityContextException ex,
            HttpServletRequest request) {

        log.error("Security context error", ex);

        return buildResponse(HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(SnsOperationException.class)
    public ResponseEntity<?> handleSnsError(
            SnsOperationException ex,
            HttpServletRequest request) {

        log.error("SNS operation failed", ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Notification service failure",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralError(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request);
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access denied: insufficient permissions",
                request
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("Endpoint not found: {}", request.getRequestURI());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Endpoint not found",
                request
        );
    }
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        ApiErrorResponse error = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, status);
    }
    @ExceptionHandler(NotificationCreationException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationCreationException(
            NotificationCreationException ex
    ) {

        log.error("Notification creation error: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "NOTIFICATION_CREATION_FAILED");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }


}