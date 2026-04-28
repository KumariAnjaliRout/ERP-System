package com.erp.erpsystem.service;

import com.erp.erpsystem.entity.Role;
import com.erp.erpsystem.entity.User;
import com.erp.erpsystem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SuperAdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminInitializer(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (userRepository.findByEmail("superadmin@erp.com").isEmpty()) {
            createSuperAdmin();
        } else {
            log.info("Super Admin already exists — skipping initialization");
        }
    }

    private void createSuperAdmin() {
        try {
            User superAdmin = User.builder()
                    .email("superadmin@erp.com")
                    .password(passwordEncoder.encode("Admin123"))
                    .role(Role.SUPER_ADMIN)
                    .username("SuperAdmin")
                    .organizationId(null)
                    .isActive(true)
                    .build();

            userRepository.save(superAdmin);
            log.info("Super Admin created successfully — email: superadmin@erp.com");
        } catch (Exception e) {
            log.error("Failed to create Super Admin: {}", e.getMessage(), e);
        }
    }
}