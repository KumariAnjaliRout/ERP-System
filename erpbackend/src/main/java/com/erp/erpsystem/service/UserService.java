package com.erp.erpsystem.service;

import com.erp.erpsystem.dto.AuthUserResponse;
import com.erp.erpsystem.dto.UpdateUserRequest;
import com.erp.erpsystem.dto.UserResponse;
import com.erp.erpsystem.dto.UserSummaryResponse;
import com.erp.erpsystem.entity.Outlet;
import com.erp.erpsystem.entity.Role;
import com.erp.erpsystem.entity.User;
import com.erp.erpsystem.exception.*;
import com.erp.erpsystem.repository.OrganizationRepository;
import com.erp.erpsystem.repository.OutletRepository;
import com.erp.erpsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j                          // FIX #17 — added logging
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OutletRepository outletRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordPolicyService passwordPolicyService;
    private final RefreshTokenService refreshTokenService;

    // ── READ METHODS ──────────────────────────────────────────────────────────

    /**
     * FIX #6 — removed direction parameter entirely.
     * Sort is now fully determined by the Pageable built in the controller.
     * Service no longer needs to know about sort direction — single responsibility.
     */
    public Page<UserResponse> getUsersPaginated(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    public UserResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    /**
     * FIX #13 — added currentOrgId parameter.
     * Was: fetched admin from DB to get orgId → extra DB call.
     * Now: orgId passed in from controller auth context — zero extra DB calls.
     */
    public UserResponse getUserById(UUID userId, UUID currentUserId,
                                    String currentRole, String currentOrgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("SUPER_ADMIN".equals(currentRole) || "SUPER_ACCOUNTANT".equals(currentRole)) {
            return toUserResponse(user);
        }
        if ("ADMIN".equals(currentRole)) {
            if (!currentOrgId.equals(user.getOrganizationId())) {
                log.warn("ADMIN from org '{}' attempted to view user from org '{}'",
                        currentOrgId, user.getOrganizationId());
                throw new ForbiddenException("Cannot view user from different organization");
            }
            return toUserResponse(user);
        }
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("Access denied. Can only view your own profile");
        }
        return toUserResponse(user);
    }

    public Page<UserResponse> getUsersByRole(String userRole, String orgId,
                                             String role, Pageable pageable) {
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }

        if ("SUPER_ADMIN".equals(userRole)) {
            return userRepository.findByRole(roleEnum, pageable).map(this::toUserResponse);
        }

        if (orgId == null || orgId.isBlank())
            throw new BadRequestException("Caller does not belong to any organization");

        return userRepository.findByOrganizationIdAndRole(orgId, roleEnum, pageable)
                .map(this::toUserResponse);
    }

    public Page<UserResponse> getUsersByOrganization(String orgId, String callerRole,
                                                     String callerOrgId, Pageable pageable) {
        if (!"SUPER_ADMIN".equals(callerRole) && !orgId.equals(callerOrgId)) {
            log.warn("Caller from org '{}' attempted to access users of org '{}'",
                    callerOrgId, orgId);
            throw new ForbiddenException("Access denied to other organization's users");
        }
        return userRepository.findAllByOrganizationId(orgId, pageable)
                .map(this::toUserResponse);
    }

    public Map<String, Object> getUserStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return Map.of(
                "userId",         user.getId(),
                "email",          user.getEmail(),
                "role",           user.getRole().name(),
                "isActive",       user.isActive(),
                "organizationId", user.getOrganizationId() != null
                        ? user.getOrganizationId() : "");
    }

    public Page<UserResponse> getMyOrganizationUsers(String orgId, Pageable pageable) {
        // FIX #12 — null guard improved: callerOrgId null is now handled
        // in the controller before reaching here with a clear BadRequestException.
        if (orgId == null || orgId.isBlank())
            throw new BadRequestException("Caller does not belong to any organization");
        return userRepository.findAllByOrganizationId(orgId, pageable)
                .map(this::toUserResponse);
    }

    public Page<UserResponse> getUsersByStatus(String userRole, String userOrgId,
                                               boolean isActive, String orgId,
                                               Pageable pageable) {
        if ("SUPER_ADMIN".equals(userRole)) {
            if (orgId != null && !orgId.isBlank())
                return userRepository.findByOrganizationIdAndIsActive(orgId, isActive, pageable)
                        .map(this::toUserResponse);
            return userRepository.findByIsActive(isActive, pageable)
                    .map(this::toUserResponse);
        }
        // FIX #19 — orgId param for non-SUPER_ADMIN is blocked in controller;
        // service always uses callerOrgId for non-SUPER_ADMIN.
        return userRepository.findByOrganizationIdAndIsActive(userOrgId, isActive, pageable)
                .map(this::toUserResponse);
    }

    /**
     * FIX #3 — was: controller fetched user then did auth check with RuntimeException.
     * FIX #4 — was: DB fetch before authorization.
     * FIX #5 — was: empty if block for SUPER_ADMIN/SUPER_ACCOUNTANT.
     * Now: fetch and auth check co-located in service; proper ForbiddenException used.
     */
    public AuthUserResponse getUserByEmail(String email, UUID callerId,
                                           String callerRole, String callerOrgId) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        // SUPER_ADMIN and SUPER_ACCOUNTANT can view any user — no restriction
        if ("SUPER_ADMIN".equals(callerRole) || "SUPER_ACCOUNTANT".equals(callerRole)) {
            return toAuthUserResponse(user);
        }

        // ADMIN and HR can view users within their own org only
        if ("ADMIN".equals(callerRole) || "HR".equals(callerRole)) {
            if (!callerOrgId.equals(user.getOrganizationId())) {
                log.warn("Role '{}' from org '{}' attempted to view user from org '{}'",
                        callerRole, callerOrgId, user.getOrganizationId());
                throw new ForbiddenException(
                        "Access denied: cannot view users from another organization");
            }
            return toAuthUserResponse(user);
        }

        // All other roles can only look up their own email
        if (!user.getId().equals(callerId)) {
            log.warn("UserId '{}' attempted to look up email belonging to userId '{}'",
                    callerId, user.getId());
            throw new ForbiddenException("Access denied: you can only search your own email");
        }

        return toAuthUserResponse(user);
    }

    // ── WRITE METHODS ─────────────────────────────────────────────────────────

    /**
     * FIX #10 — save only called when changes are non-empty.
     * FIX #14 — audit log now uses normalized new email, consistent with DB.
     * FIX #15 — username change now logs old → new value.
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request,
                                   UUID performedBy, String performedByRole,
                                   String performedByOrgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("ADMIN".equals(performedByRole) &&
                !user.getOrganizationId().equals(performedByOrgId)) {
            throw new ForbiddenException("Cannot update user from different organization");
        }

        StringBuilder changes = new StringBuilder();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!user.getEmail().equals(newEmail)) {
                if (userRepository.existsByEmail(newEmail))
                    throw new DuplicateResourceException("Email already exists");
                // FIX #14 — log normalized new email, consistent with what's stored
                changes.append("email: '").append(user.getEmail())
                        .append("' → '").append(newEmail).append("'; ");
                user.updateEmail(newEmail);
            }
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equals(user.getUsername())) {
                // FIX #15 — log old → new username
                changes.append("username: '").append(user.getUsername())
                        .append("' → '").append(newUsername).append("'; ");
                user.updateUsername(newUsername);
            }
        }

        // FIX #10 — only save and audit when something actually changed
        if (changes.isEmpty()) {
            log.debug("No changes detected for userId={}, skipping save", userId);
            return toUserResponse(user);
        }

        User saved = userRepository.save(user);
        auditService.logUpdate(performedBy, performedByRole, performedByOrgId,
                "USER", userId.toString(), saved.getEmail(), changes.toString());

        log.info("User updated: userId={}, changes={}, by={}", userId, changes, performedBy);
        return toUserResponse(saved);
    }

    @Transactional
    public void removeUserOutlet(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOutletId() != null) {
            outletRepository.findById(user.getOutletId()).ifPresent(outlet -> {
                outlet.setOutletOwnerId(null);
                outletRepository.save(outlet);
            });
            user.removeFromOutlet();
            userRepository.save(user);
            log.info("Outlet assignment removed for userId={}", userId);
        }
    }

    /**
     * FIX #9  — role enum validation now happens BEFORE uniqueness DB check.
     * FIX #18 — fixed inconsistent indentation throughout.
     * Added callerOrgId parameter for cross-org validation without extra DB call.
     */
    @Transactional
    public void updateUserRole(UUID userId, String newRole, String newOutletId,
                               UUID performedBy, String performedByRole,
                               String performedByOrgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // FIX #9 — validate role enum FIRST before any DB uniqueness checks
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + newRole);
        }

        if (roleEnum == Role.SUPER_ADMIN)
            throw new ForbiddenException("Cannot assign SUPER_ADMIN role");

        if ("ADMIN".equals(performedByRole) &&
                (roleEnum == Role.ADMIN || roleEnum == Role.SUPER_ACCOUNTANT))
            throw new ForbiddenException("ADMIN cannot assign ADMIN or higher roles");

        // Uniqueness check now after role is validated
        if (roleEnum == Role.ACCOUNTANT) {
            if (userRepository.existsByOrganizationIdAndRoleAndIdNot(
                    user.getOrganizationId(), Role.ACCOUNTANT, userId))
                throw new DuplicateResourceException(
                        "An Accountant already exists in this organization. Only one is allowed.");
        }

        String oldRole = user.getRole().name();

        // Unlink from outlet if changing away from OUTLET role
        if (user.getRole() == Role.OUTLET && roleEnum != Role.OUTLET) {
            if (user.getOutletId() != null) {
                outletRepository.findById(user.getOutletId()).ifPresent(outlet -> {
                    outlet.assignOwner(null);
                    outletRepository.save(outlet);
                });
                user.removeFromOutlet();
            }
        }

        if (roleEnum == Role.OUTLET) {
            if (newOutletId == null || newOutletId.isBlank())
                throw new BadRequestException(
                        "outletId is required when assigning OUTLET role");

            Outlet outlet = outletRepository.findById(newOutletId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Outlet '" + newOutletId + "' does not exist"));

            if (!outlet.getIsActive())
                throw new BadRequestException(
                        "Cannot assign user to inactive outlet '" + newOutletId + "'");

            if (!user.getOrganizationId().equals(outlet.getOrganizationId()))
                throw new ForbiddenException(
                        "User and outlet must belong to the same organization");

            if (userRepository.existsByOutletIdAndIsActiveTrue(newOutletId))
                throw new DuplicateResourceException(
                        "An Outlet Owner already exists for this outlet. Only one is allowed.");

            user.assignToOutlet(newOutletId);
            outlet.setOutletOwnerId(userId);
            outletRepository.save(outlet);
        }

        user.updateRole(roleEnum);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);

        auditService.logUpdate(performedBy, performedByRole, user.getOrganizationId(),
                "USER", user.getId().toString(), user.getEmail(),
                "Role changed: '" + oldRole + "' → '" + newRole + "'");

        log.info("User role updated: userId={}, {} → {}, by={}", userId, oldRole, newRole, performedBy);
    }

    /**
     * FIX #8  — removed performer DB re-fetch. Role is now passed as a parameter
     * from the controller auth context — @PreAuthorize already enforces role access.
     * FIX #17 — added logging.
     */
    @Transactional
    public void toggleUserActivation(UUID userId, boolean activate,
                                     UUID performedBy, String performedByRole,
                                     String performedByOrgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // FIX #8 — @PreAuthorize handles role access; no need to re-check here.
        // Org-scope checks use the passed performedByOrgId, not a DB-fetched performer.
        if ("HR".equals(performedByRole)) {
            if (!performedByOrgId.equals(user.getOrganizationId()))
                throw new ForbiddenException("Cannot manage users in a different organization");
            if (user.getRole() != Role.EMPLOYEE)
                throw new ForbiddenException("HR can only manage EMPLOYEE accounts");
        }

        if ("ADMIN".equals(performedByRole) &&
                !performedByOrgId.equals(user.getOrganizationId()))
            throw new ForbiddenException("Cannot manage users in a different organization");

        if (activate && user.getOrganizationId() != null) {
            organizationRepository.findById(user.getOrganizationId())
                    .ifPresent(org -> {
                        if (!org.isActive())
                            throw new BadRequestException(
                                    "Cannot activate user. Organization '"
                                            + org.getId() + " - " + org.getName()
                                            + "' is deactivated. Reactivate the organization first.");
                    });
        }

        if (activate) {
            user.activate();
        } else {
            user.deactivate();
            if (user.getRole() == Role.OUTLET && user.getOutletId() != null) {
                outletRepository.findById(user.getOutletId()).ifPresent(outlet -> {
                    outlet.assignOwner(null);
                    outletRepository.save(outlet);
                });
            }
            refreshTokenService.revokeAllUserTokens(user);
        }

        userRepository.save(user);

        auditService.logAction(performedBy, performedByRole, performedByOrgId,
                activate ? "ACTIVATE_USER" : "DEACTIVATE_USER",
                "USER", userId.toString(),
                (activate ? "Activated" : "Deactivated") + " user: " + user.getEmail());

        log.info("User {}: userId={}, by={}", activate ? "activated" : "deactivated",
                userId, performedBy);
    }

    /**
     * FIX #16 — was: logDelete() creating "DELETE_USER" audit entry.
     * Now: logAction("SOFT_DELETE_USER") — accurately reflects that the user
     * record still exists in the DB and was only deactivated.
     */
    @Transactional
    public void deleteUser(UUID userId, UUID performedBy,
                           String performedByRole, String userOrgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN)
            throw new ForbiddenException("Cannot delete SUPER_ADMIN account");
        if ("ADMIN".equals(performedByRole) && user.getRole() == Role.ADMIN)
            throw new ForbiddenException("ADMIN cannot delete another ADMIN");
        if ("ADMIN".equals(performedByRole) &&
                !user.getOrganizationId().equals(userOrgId))
            throw new ForbiddenException("Cannot delete user from different organization");

        if (user.getRole() == Role.OUTLET && user.getOutletId() != null) {
            outletRepository.findById(user.getOutletId()).ifPresent(outlet -> {
                outlet.assignOwner(null);
                outletRepository.save(outlet);
            });
        }

        user.deactivate();
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);

        // FIX #16 — soft delete logged accurately
        auditService.logAction(performedBy, performedByRole, userOrgId,
                "SOFT_DELETE_USER", "USER", userId.toString(),
                "Soft-deleted (deactivated) user: " + user.getEmail());

        log.info("User soft-deleted: userId={}, by={}", userId, performedBy);
    }

    /**
     * FIX #20 — added active outlet check before assigning user.
     */
    @Transactional
    public void updateUserOutlet(UUID userId, String outletId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.OUTLET)
            throw new ForbiddenException("Only OUTLET role users can be assigned to an outlet");

        Outlet outlet = outletRepository.findById(outletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Outlet '" + outletId + "' does not exist"));

        // FIX #20 — prevent assignment to inactive outlets
        if (!outlet.getIsActive())
            throw new BadRequestException(
                    "Cannot assign user to inactive outlet '" + outletId + "'");

        if (!user.getOrganizationId().equals(outlet.getOrganizationId()))
            throw new ForbiddenException(
                    "User and outlet must belong to the same organization");

        if (user.getOutletId() != null && !user.getOutletId().isBlank())
            throw new DuplicateResourceException(
                    "This user is already assigned to outlet '"
                            + user.getOutletId() + "'. Remove existing outlet first.");

        if (userRepository.existsByOutletIdAndIsActiveTrue(outletId))
            throw new DuplicateResourceException(
                    "An Outlet Owner already exists for this outlet. Only one is allowed.");

        user.assignToOutlet(outletId);
        userRepository.save(user);
        outlet.setOutletOwnerId(userId);
        outletRepository.save(outlet);

        log.info("User outlet updated: userId={}, outletId={}", userId, outletId);
    }

    @Transactional
    public void resetUserPassword(UUID userId, String newPassword,
                                  UUID performedBy, String performedByRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN)
            throw new ForbiddenException("Super Admin password cannot be reset");

        passwordPolicyService.validatePassword(newPassword);
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);

        auditService.logAction(performedBy, performedByRole, user.getOrganizationId(),
                "RESET_PASSWORD", "USER", userId.toString(),
                "Password reset for user: " + user.getEmail());

        log.info("Password reset for userId={}, by={}", userId, performedBy);
    }

    @Transactional
    public void resetOrgUserPassword(UUID userId, String newPassword,
                                     String adminOrgId, UUID performedBy,
                                     String performedByRole) {
        if (adminOrgId == null || adminOrgId.isBlank())
            throw new BadRequestException("You do not belong to any organization");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!adminOrgId.equals(user.getOrganizationId()))
            throw new ForbiddenException(
                    "Cannot reset password of user in another organization");

        passwordPolicyService.validatePassword(newPassword);
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);

        auditService.logAction(performedBy, performedByRole, adminOrgId,
                "ORG_RESET_PASSWORD", "USER", userId.toString(),
                "Password reset by ADMIN for user: " + user.getEmail());

        log.info("Org password reset for userId={}, by={}", userId, performedBy);
    }

    public UserSummaryResponse getInternalUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toSummary(user);
    }

    public List<UserSummaryResponse> getUsersByRoleAndOrganization(String role,
                                                                   String organizationId) {
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
        return userRepository.findByOrganizationIdAndRole(organizationId, roleEnum)
                .stream().map(this::toSummary).toList();
    }

    public List<UserSummaryResponse> getSuperAdmins() {
        return userRepository.findByRole(Role.SUPER_ADMIN)
                .stream().map(this::toSummary).toList();
    }

    public List<UserSummaryResponse> getSuperAccountants() {
        return userRepository.findByRole(Role.SUPER_ACCOUNTANT)
                .stream().map(this::toSummary).toList();
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .organizationId(user.getOrganizationId())
                .isActive(user.isActive())
                .build();
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .organizationId(user.getOrganizationId())
                .outletId(user.getOutletId())
                .active(user.isActive())
                .build();
    }

    private UserSummaryResponse toSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .organizationId(user.getOrganizationId())
                .outletId(user.getOutletId())
                .active(user.isActive())
                .build();
    }
}