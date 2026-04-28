package com.erp.accountantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableFeignClients(basePackages = "com.erp.accountantservice.client")
public class AccountantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountantServiceApplication.class, args);
    }
}
