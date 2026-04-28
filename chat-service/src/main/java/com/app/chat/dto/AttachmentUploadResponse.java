package com.app.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentUploadResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;   // presigned URL (temporary)
}
