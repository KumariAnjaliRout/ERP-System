package com.app.notification.config;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Slf4j
@Configuration
public class AwsSnsConfig {

    @Value("${aws.region}")
    private String region;

    @PostConstruct
    public void validate() {
        if (region == null || region.isBlank()) {
            throw new IllegalStateException("AWS region is not configured. Set aws.region in application.yml");
        }
        log.info("Initializing AWS SNS client with region: {}", region);
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
