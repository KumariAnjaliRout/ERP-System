package com.app.EMS.exception;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ApiError {

    private String message;
    private String code;
    private int status;
    private String path;
    private LocalDateTime timestamp;
}
