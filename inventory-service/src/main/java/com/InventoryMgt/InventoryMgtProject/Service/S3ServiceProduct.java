package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Expection.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceProduct {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.credentials.region}")
    private String region;

    // ================= UPLOAD =================
    public String uploadFile(MultipartFile file, String folder) {

        try {
            String safeName = UUID.randomUUID() + "_" +
                    file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", "_");

            String key = folder + "/" + safeName;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return key; // ✅ return key, NOT URL

        } catch (Exception ex) {
            log.error("S3 upload failed", ex);
            throw new FileStorageException("File upload failed", ex);
        }
    }

    // ================= DELETE =================
    public void deleteFile(String key) {

        if (key == null || key.isBlank()) return;

        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
        } catch (Exception ex) {
            log.warn("Failed to delete file from S3: {}", key, ex);
        }
    }

    // ================= GET FILE URL =================
    public String getFileUrl(String key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}

