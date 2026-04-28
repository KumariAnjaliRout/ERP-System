package com.erp.erpsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ErpSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpSystemApplication.class, args);
        System.out.println("✅ ERP System Started!");
    }
}