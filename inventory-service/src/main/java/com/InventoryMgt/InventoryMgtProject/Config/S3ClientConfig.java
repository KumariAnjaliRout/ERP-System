package com.InventoryMgt.InventoryMgtProject.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3ClientConfig {

    @Value("${cloud.aws.credentials.access-key}")
    private String accesskey;
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretkey;
    @Value("${cloud.aws.credentials.region}")
    private String awsRegion;
    @Bean
    public S3Client s3Client(){
        AwsBasicCredentials awsCredentials =
                AwsBasicCredentials.create(accesskey, secretkey);
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(awsCredentials)
                )
                .build();

    }
}
