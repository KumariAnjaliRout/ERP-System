package com.app.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws.sns")
@Getter
@Setter
public class AwsSnsProperties {

    private String platformArn;
}

