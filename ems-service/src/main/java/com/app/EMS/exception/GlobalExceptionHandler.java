package com.app.EMS.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ---------- NOT FOUND ---------- */

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request){

        return build(ex.getMessage(), ErrorCode.NOT_FOUND,404,request);
    }


    /* ---------- DUPLICATE ---------- */

    @ExceptionHandler(AlreadyExistsResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            AlreadyExistsResourceException ex,
            HttpServletRequest request){

        return build(ex.getMessage(), ErrorCode.DUPLICATE,409,request);
    }


    /* ---------- BAD REQUEST ---------- */

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request){

        return build(ex.getMessage(), ErrorCode.BAD_REQUEST,400,request);
    }


    /* ---------- VALIDATION ---------- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request){

        String msg =
                ex.getBindingResult()
                        .getFieldError()
                        .getDefaultMessage();

        return build(msg, ErrorCode.VALIDATION_FAILED,400,request);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidJson(HttpMessageNotReadableException ex,  HttpServletRequest request) {

        String message = "Invalid request format";

        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException formatEx) {

            if (formatEx.getTargetType().equals(java.time.LocalDate.class)) {
                message = "Invalid date format or invalid calendar date. Use yyyy-MM-dd and valid dates only.";
            }
        }

        if (cause instanceof DateTimeParseException) {
            message = "Invalid date value. Please check day/month/year.";
        }

        return build(message,ErrorCode.BAD_REQUEST, 400,request);
    }



    /* ---------- SECURITY ---------- */

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request){

        return build(ex.getMessage(), ErrorCode.UNAUTHORIZED,401,request);
    }


    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            org.springframework.security.authorization.AuthorizationDeniedException ex,
            HttpServletRequest request){

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(
                        "Access Denied",
                        "FORBIDDEN",
                        403,
                        request.getRequestURI(),
                        LocalDateTime.now()
                ));
    }



    /* ---------- FALLBACK ---------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(
            Exception ex,
            HttpServletRequest request){

        return build("Something went wrong",
                ErrorCode.SERVER_ERROR,500,request);
    }


    /* ---------- BUILDER ---------- */

    private ResponseEntity<ApiError> build(
            String message,
            ErrorCode code,
            int status,
            HttpServletRequest request){

        return ResponseEntity.status(status)
                .body(ApiError.builder()
                        .message(message)
                        .code(code.name())
                        .status(status)
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
