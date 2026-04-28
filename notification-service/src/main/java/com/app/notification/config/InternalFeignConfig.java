package com.app.notification.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalFeignConfig {

    private final String internalSecret;

    public InternalFeignConfig(@Value("${internal.api.secret}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Bean
    public RequestInterceptor internalSecretInterceptor() {
        return template ->
                template.header("X-Internal-Secret", internalSecret);
    }
}
