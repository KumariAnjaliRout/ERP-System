package com.erp.erpsystem.service;

import com.erp.erpsystem.dto.*;
import com.erp.erpsystem.entity.*;
import com.erp.erpsystem.exception.*;
import com.erp.erpsystem.repository.OrganizationRepository;
import com.erp.erpsystem.repository.OutletRepository;
import com.erp.erpsystem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutletRepository outletRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final PasswordPolicyService passwordPolicyService;
    private final UserEventPublisher userEventPublisher;
    private final RefreshTokenService refreshTokenService;

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

     private static final String NO_ORGANIZATION = null;

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void validateOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.trim().isEmpty())
            throw new BadRequestException("Organization ID is required");
        if (UUID_PATTERN.matcher(organizationId).matches())
            throw new BadRequestException(
                    "Invalid organization ID format. Use a readable string (e.g., 'ORG001'), not a UUID.");
    }

    private String generateAccessToken(User user) {
        if (user.getRole() == Role.OUTLET) {
            if (user.getOutletId() == null)
                throw new BadRequestException("Outlet Owner does not have an outlet assigned");
            return jwtService.generateToken(user, user.getOutletId());
        }
        return jwtService.generateToken(user, null);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken  = generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getRemainingTimeInSeconds(accessToken))
                .build();
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> getCallerDetailsFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated context available in service layer");
        }
        Object details = auth.getDetails();
        if (!(details instanceof Map)) {
            throw new IllegalStateException("Unexpected authentication details format in service layer");
        }
        return (Map<String, Object>) details;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────


    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
         String normalizedEmail = request.getEmail().toLowerCase().trim();

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
             auditService.logFailedLogin(normalizedEmail, "Email not found", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.logFailedLogin(normalizedEmail, "Invalid password", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isActive()) {
            auditService.logFailedLogin(normalizedEmail, "Account deactivated", ipAddress, userAgent);
            throw new UnauthorizedException("Account is deactivated");
        }

        if (user.getOrganizationId() != null &&
                UUID_PATTERN.matcher(user.getOrganizationId()).matches()) {
            log.error("User {} has invalid UUID-format organizationId: {}",
                    user.getId(), user.getOrganizationId());
            throw new BadRequestException(
                    "Your account has an invalid organization ID. Please contact your administrator.");
        }

        auditService.logSuccessfulLogin(
                user.getId(), user.getRole().name(),
                user.getOrganizationId(), normalizedEmail, ipAddress, userAgent);

        log.info("Successful login for userId={}, role={}", user.getId(), user.getRole());
        return buildLoginResponse(user);
    }

    // ── REFRESH TOKEN ─────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();

        if (!user.isActive())
            throw new UnauthorizedException("Account is deactivated");

        String newAccessToken = generateAccessToken(user);
        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(jwtService.getRemainingTimeInSeconds(newAccessToken))
                .build();
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String refreshTokenString) {
        refreshTokenService.revokeToken(refreshTokenString);
        log.debug("Refresh token revoked");
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────────


    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        Map<String, Object> callerDetails = getCallerDetailsFromContext();

        String rawUserId      = (String) callerDetails.get("userId");
        String userRole       = (String) callerDetails.get("role");
        String organizationId = (String) callerDetails.get("organizationId");

        if (rawUserId == null || rawUserId.isBlank())
            throw new IllegalStateException("userId not found in security context");

        UUID userId;
        try {
            userId = UUID.fromString(rawUserId);
        } catch (IllegalArgumentException ex) {
            log.error("Malformed userId in security context: {}", rawUserId);
            throw new IllegalStateException("Malformed userId in authentication context");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN)
            throw new ForbiddenException("Super Admin password cannot be changed");

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            auditService.logFailedPasswordChange(
                    userId, userRole, organizationId,
                    user.getEmail(), "Incorrect old password");
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new BadRequestException("New password and confirm password do not match");

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword()))
            throw new BadRequestException("New password cannot be the same as old password");

        passwordPolicyService.validatePassword(request.getNewPassword());
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);

        auditService.logSuccessfulPasswordChange(userId, userRole, organizationId, user.getEmail());
        log.info("Password changed for userId={}", userId);

        return ChangePasswordResponse.builder()
                .message("Password changed successfully. Please login again.")
                .email(user.getEmail())
                .build();
    }

    // ── CREATE ADMIN ──────────────────────────────────────────────────────────

    @Transactional
    public CreateUserResponse createAdmin(CreateAdminRequest request,
                                          UUID performedBy,
                                          String performedByRole) {
        validateOrganizationId(request.getOrganizationId());

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization '" + request.getOrganizationId() + "' does not exist."));

        if (!org.isActive())
            throw new BadRequestException("Organization is not active");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already exists");

        passwordPolicyService.validatePassword(request.getPassword());

        User admin = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .organizationId(request.getOrganizationId())
                .isActive(true)
                .createdBy(performedBy)
                .build();

        User savedAdmin = userRepository.save(admin);
        userEventPublisher.publishUserCreatedEvent(savedAdmin);

        auditService.logCreate(performedBy, performedByRole, savedAdmin.getOrganizationId(),
                "ADMIN", savedAdmin.getId().toString(), savedAdmin.getEmail());

        log.info("Admin created: userId={}, orgId={}, by={}",
                savedAdmin.getId(), savedAdmin.getOrganizationId(), performedBy);

     return CreateUserResponse.builder()
                .userId(savedAdmin.getId().toString())
                .email(savedAdmin.getEmail())
                .username(savedAdmin.getUsername())
                .role(savedAdmin.getRole().name())
                .organizationId(savedAdmin.getOrganizationId())
                .message("Admin created successfully for organization: " + org.getName())
                .build();
    }

    // ── CREATE SUPER ACCOUNTANT ───────────────────────────────────────────────

    @Transactional
    public CreateUserResponse createSuperAccountant(CreateSuperAccountantRequest request,
                                                    UUID performedBy,
                                                    String performedByRole) {
        if (userRepository.existsByRole(Role.SUPER_ACCOUNTANT))
            throw new DuplicateResourceException(
                    "A Super Accountant already exists in the system. Only one is allowed.");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already exists");

        passwordPolicyService.validatePassword(request.getPassword());

        User superAccountant = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.SUPER_ACCOUNTANT)
                .isActive(true)
                .createdBy(performedBy)
                .build();

        User saved = userRepository.save(superAccountant);
        userEventPublisher.publishUserCreatedEvent(saved);

        // FIX #16 — NO_ORGANIZATION constant documents intent clearly
        auditService.logCreate(performedBy, performedByRole, NO_ORGANIZATION,
                "SUPER_ACCOUNTANT", saved.getId().toString(), saved.getEmail());

        log.info("Super Accountant created: userId={}, by={}", saved.getId(), performedBy);

        return CreateUserResponse.builder()
                .userId(saved.getId().toString())
                .email(saved.getEmail())
                .username(saved.getUsername())
                .role(saved.getRole().name())
                .message("Super Accountant created successfully")
                .build();
    }

    // ── CREATE USER IN ORGANIZATION ───────────────────────────────────────────

    @Transactional
    public CreateUserResponse createUserInOrganization(CreateUserRequest request,
                                                       UUID callerUserId,
                                                       String callerRole,
                                                       String callerOrgId) {
        String normalizedRole = callerRole.startsWith("ROLE_")
                ? callerRole.substring(5) : callerRole;

        String targetOrgId;
        if ("SUPER_ADMIN".equals(normalizedRole)) {
            targetOrgId = request.getOrganizationId();
            if (targetOrgId == null || targetOrgId.isBlank())
                throw new BadRequestException(
                        "organizationId is required when SUPER_ADMIN creates a user");
            Organization org = organizationRepository.findById(targetOrgId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Organization '" + targetOrgId + "' does not exist."));
            if (!org.isActive())
                throw new BadRequestException("Organization is not active");
        } else {
            targetOrgId = callerOrgId;
            if (targetOrgId == null || targetOrgId.isBlank())
                throw new BadRequestException("Caller organization could not be determined");
            if (request.getOrganizationId() != null && !request.getOrganizationId().isBlank())
                throw new BadRequestException(
                        "ADMIN cannot specify organizationId. Your organization is automatically assigned.");
        }

        validateOrganizationId(targetOrgId);

        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already exists");

        switch (normalizedRole) {
            case "SUPER_ADMIN" -> {
                if (request.getRole() == Role.SUPER_ADMIN ||
                        request.getRole() == Role.SUPER_ACCOUNTANT)
                    throw new ForbiddenException(
                            "Use the dedicated endpoints to create SUPER_ADMIN or SUPER_ACCOUNTANT");
                if (request.getRole() == Role.ADMIN)
                    throw new ForbiddenException(
                            "Use the dedicated endpoint to create ADMIN");
            }
            case "ADMIN" -> {
                if (request.getRole() != Role.MANAGER &&
                        request.getRole() != Role.HR &&
                        request.getRole() != Role.ACCOUNTANT &&
                        request.getRole() != Role.OUTLET &&
                        request.getRole() != Role.EMPLOYEE)
                    throw new ForbiddenException(
                            "ADMIN can only create MANAGER, HR, ACCOUNTANT, OUTLET or EMPLOYEE roles");
            }
            case "HR" -> {
                if (request.getRole() != Role.EMPLOYEE)
                    throw new ForbiddenException("HR can only create EMPLOYEE role");
            }
            default -> throw new ForbiddenException("You do not have permission to create users");
        }

        if (request.getRole() == Role.ACCOUNTANT) {
            if (userRepository.existsByOrganizationIdAndRole(targetOrgId, Role.ACCOUNTANT))
                throw new DuplicateResourceException(
                        "An Accountant already exists in this organization. Only one is allowed.");
        }

         if (request.getRole() == Role.OUTLET) {
            if (request.getOutletId() == null || request.getOutletId().isBlank())
                throw new BadRequestException("outletId is required when creating an Outlet Owner");

            Outlet outlet = outletRepository.findById(request.getOutletId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Outlet '" + request.getOutletId() + "' does not exist"));

            if (userRepository.existsByOutletIdAndIsActiveTrue(request.getOutletId()))
                throw new DuplicateResourceException(
                        "An Outlet Owner already exists for this outlet. Only one is allowed.");

            passwordPolicyService.validatePassword(request.getPassword());

            User user = User.builder()
                    .email(request.getEmail().toLowerCase().trim())
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .organizationId(targetOrgId)
                    .outletId(request.getOutletId())
                    .isActive(true)
                    .createdBy(callerUserId)
                    .build();

            User savedUser = userRepository.save(user);
            userEventPublisher.publishUserCreatedEvent(savedUser);

             outlet.assignOwner(savedUser.getId());
            outletRepository.save(outlet);

            auditService.logCreate(callerUserId, normalizedRole, targetOrgId,
                    savedUser.getRole().name(), savedUser.getId().toString(), savedUser.getEmail());

            log.info("Outlet user created: userId={}, outletId={}, by={}",
                    savedUser.getId(), request.getOutletId(), callerUserId);

            return CreateUserResponse.builder()
                    .userId(savedUser.getId().toString())
                    .email(savedUser.getEmail())
                    .username(savedUser.getUsername())
                    .role(savedUser.getRole().name())
                    .organizationId(savedUser.getOrganizationId())
                    .message("User created successfully")
                    .build();
        }


        passwordPolicyService.validatePassword(request.getPassword());

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .organizationId(targetOrgId)
                .outletId(null)
                .isActive(true)
                .createdBy(callerUserId)
                .build();

        User savedUser = userRepository.save(user);
        userEventPublisher.publishUserCreatedEvent(savedUser);

        auditService.logCreate(callerUserId, normalizedRole, targetOrgId,
                savedUser.getRole().name(), savedUser.getId().toString(), savedUser.getEmail());

        log.info("User created: userId={}, role={}, orgId={}, by={}",
                savedUser.getId(), savedUser.getRole(), targetOrgId, callerUserId);

        return CreateUserResponse.builder()
                .userId(savedUser.getId().toString())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .organizationId(savedUser.getOrganizationId())
                .message("User created successfully")
                .build();
    }

    // ── GET USER INFO ─────────────────────────────────────────────────────────


    public AuthUserResponse getUserInfoById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .organizationId(user.getOrganizationId())
                .outletId(user.getRole() == Role.OUTLET ? user.getOutletId() : null)
                .active(user.isActive())
                .build();
    }

    // ── PRIVATE UTILITIES ─────────────────────────────────────────────────────


    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated list; first entry is the client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}