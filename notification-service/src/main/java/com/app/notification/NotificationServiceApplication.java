package com.app.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication(
		exclude = {
				org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
		}
)
@EnableFeignClients
@EnableCaching
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
