package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Entities.OrderItem;
import com.InventoryMgt.InventoryMgtProject.Expection.InvoiceNotGeneratedException;
import com.InventoryMgt.InventoryMgtProject.Expection.InvoiceUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3InvoiceService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.credentials.region}")
    private String region;

    public String uploadInvoice(byte[] pdf, String invoiceNo){

        String key = "invoices/" + invoiceNo + ".pdf";

        try {

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(pdf)
            );

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

        } catch (SdkException ex) {

            log.error("Failed to upload invoice {} to S3", invoiceNo, ex);

            throw new InvoiceUploadException("Failed to upload invoice", ex);
        }
    }

    public byte[] downloadInvoiceFromS3(String value) {

        try {

            if (value == null || value.isBlank()) {
                throw new InvoiceNotGeneratedException("Invalid invoice reference");
            }

            String key = value;

            // 🔥 Handle OLD data (full S3 URL → extract key)
            if (value.contains(".amazonaws.com/")) {
                key = value.substring(value.indexOf(".amazonaws.com/") + 15);
            }

            log.info("Downloading invoice from S3 with key: {}", key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(request).asByteArray();

        } catch (NoSuchKeyException ex) {

            log.error("File not found in S3 for key/value: {}", value);
            throw new InvoiceNotGeneratedException("Invoice file not found");

        } catch (S3Exception ex) {

            log.error("S3 error while fetching file: {}", value, ex);
            throw new RuntimeException("Failed to fetch file from S3");

        } catch (Exception ex) {

            log.error("Unexpected error while downloading invoice: {}", value, ex);
            throw new RuntimeException("Unexpected error while downloading invoice");
        }
    }
}