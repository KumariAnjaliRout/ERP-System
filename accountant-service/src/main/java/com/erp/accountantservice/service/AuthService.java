package com.erp.accountantservice.service;

import com.erp.accountantservice.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    public UserDTO getUserFromHeaders(String userId, String userRole, String branchId) {
        log.info("Getting user from headers - ID: {}, Role: {}, Branch: {}", userId, userRole, branchId);

        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setRole(userRole);
        user.setBranchId(branchId);
        user.setEmail("test@test.com");
        user.setName("Test User");
        user.setActive(true);

        return user;
    }

}