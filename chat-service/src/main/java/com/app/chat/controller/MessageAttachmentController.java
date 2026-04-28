package com.app.chat.controller;

import com.app.chat.dto.AttachmentUploadResponse;
import com.app.chat.entity.MessageAttachment;
import com.app.chat.repository.MessageAttachmentRepository;
import com.app.chat.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageAttachmentController {

    private final S3Service s3Service;
    private final MessageAttachmentRepository attachmentRepository;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AttachmentUploadResponse uploadFile(
            @RequestParam("file") MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File must not be empty");
        }

        // ✅ Upload to S3 → returns key only
        String s3Key = s3Service.uploadFile(file);

        // ✅ Save only key in DB
        MessageAttachment attachment = MessageAttachment.builder()
                .messageId(null) // will be linked later
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .s3Key(s3Key)
                .build();

        MessageAttachment saved = attachmentRepository.save(attachment);

        // ✅ Generate presigned URL dynamically
        String presignedUrl = s3Service.generatePresignedUrl(s3Key);

        // ✅ Return clean response
        return AttachmentUploadResponse.builder()
                .id(saved.getId())
                .fileName(saved.getFileName())
                .fileType(saved.getFileType())
                .fileSize(saved.getFileSize())
                .fileUrl(presignedUrl)
                .build();
    }
}
