package com.erp.erpsystem.service;

import com.erp.erpsystem.dto.CreateOrganizationRequest;
import com.erp.erpsystem.dto.OrganizationResponse;
import com.erp.erpsystem.dto.UpdateOrganizationRequest;
import com.erp.erpsystem.entity.Organization;
import com.erp.erpsystem.entity.Outlet;
import com.erp.erpsystem.entity.User;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.exception.DuplicateResourceException;
import com.erp.erpsystem.exception.ResourceNotFoundException;
import com.erp.erpsystem.repository.OrganizationRepository;
import com.erp.erpsystem.repository.OutletRepository;
import com.erp.erpsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j                          // FIX #14 — added logging
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OutletRepository outletRepository;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;

    // FIX #4 — added ^ and $ anchors to prevent partial string matches.
    // e.g. "prefix-550e8400-...-suffix" would previously match.
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void validateOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.trim().isEmpty())
            throw new BadRequestException("Organization ID is required");
        if (UUID_PATTERN.matcher(organizationId).matches())
            throw new BadRequestException(
                    "Invalid organization ID format. Use a readable string (e.g., 'ORG001'), not a UUID.");
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request,
            UUID performedBy,
            String performedByRole) {

        // FIX #11 — normalize org ID to uppercase for consistent storage.
        // "org001" and "ORG001" now resolve to the same ID, preventing silent duplicates.
        String normalizedId = request.getId().trim().toUpperCase();

        validateOrganizationId(normalizedId);

        if (organizationRepository.existsById(normalizedId))
            throw new DuplicateResourceException(
                    "Organization with ID '" + normalizedId + "' already exists");

        if (organizationRepository.existsByNameIgnoreCase(request.getName()))
            throw new DuplicateResourceException(
                    "Organization name '" + request.getName() + "' already exists");

        Organization org = Organization.builder()
                .id(normalizedId)
                .name(request.getName().trim())
                .address(request.getAddress())
                .build();

        Organization savedOrg = organizationRepository.save(org);

        // FIX #12 — was: passing null for organizationId in audit log.
        // The org being created IS the organization context — pass its id.
        auditService.logCreate(performedBy, performedByRole, savedOrg.getId(),
                "ORGANIZATION", savedOrg.getId(), savedOrg.getName());

        log.info("Organization created: id={}, name={}, by={}", savedOrg.getId(), savedOrg.getName(), performedBy);

        return mapToResponse(savedOrg);
    }

    // ── GET ALL (Paginated) ───────────────────────────────────────────────────

    public Page<OrganizationResponse> getAllOrganizationsPaginated(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("name").ascending());
        }
        return organizationRepository.findAll(pageable).map(this::mapToResponse);
    }

    // ── GET ALL (Internal, unpaginated) ──────────────────────────────────────

    public List<OrganizationResponse> getAllOrganizations() {
        // FIX #16 — added secondary sort by id for stable ordering when names collide
        return organizationRepository
                .findAll(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    public OrganizationResponse getOrganizationById(String id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with ID: " + id));
        return mapToResponse(org);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public OrganizationResponse updateOrganization(
            String id,
            UpdateOrganizationRequest request,
            UUID performedBy,
            String performedByRole) {

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with ID: " + id));

        StringBuilder changes = new StringBuilder();

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(org.getName())) {
                organizationRepository.findByNameIgnoreCase(newName)
                        .ifPresent(existing -> {
                            if (!existing.getId().equals(id))
                                throw new DuplicateResourceException(
                                        "Organization name '" + newName + "' already exists");
                        });
                changes.append("name: '").append(org.getName())
                        .append("' → '").append(newName).append("'; ");
                org.updateName(newName);
            }
        }

        if (request.getAddress() != null) {
            // FIX #17 — was: just "address updated;" with no old value shown.
            // Now: logs old address → new address for meaningful audit trail.
            changes.append("address: '").append(org.getAddress())
                    .append("' → '").append(request.getAddress()).append("'; ");
            org.updateAddress(request.getAddress());
        }

        // FIX #10 — was: organizationRepository.save(org) called unconditionally.
        // Now: only saves and audits when something actually changed.
        if (changes.isEmpty()) {
            log.debug("No changes detected for organization id={}, skipping save", id);
            return mapToResponse(org);
        }

        Organization updatedOrg = organizationRepository.save(org);

        // FIX #12 — was: passing null for organizationId in audit log.
        // Org update is scoped to this org — pass its id.
        auditService.logUpdate(performedBy, performedByRole, updatedOrg.getId(),
                "ORGANIZATION", updatedOrg.getId(), updatedOrg.getName(),
                changes.toString());

        log.info("Organization updated: id={}, changes={}, by={}", id, changes, performedBy);
        return mapToResponse(updatedOrg);
    }

    // ── TOGGLE ACTIVATION ─────────────────────────────────────────────────────

    @Transactional
    public int toggleOrganizationActivation(
            String id,
            boolean activate,
            UUID performedBy,
            String performedByRole) {

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        // FIX #18 — guard: if already in the desired state, reject the request.
        // Prevents pointless DB writes and misleading audit log entries.
        if (org.isActive() == activate) {
            throw new BadRequestException(
                    "Organization is already " + (activate ? "active" : "inactive"));
        }

        org.toggleActive(activate);
        organizationRepository.save(org);

        // FIX #6 — was: updateActiveStatusByOrganizationId (bulk update) AND
        // findByOrganizationId (full fetch) — two round-trips for the same set of users.
        // Now: fetch users once, revoke tokens from that list if deactivating,
        // then derive the count from the list — single DB read.
        List<User> orgUsers = userRepository.findByOrganizationId(id);
        int updatedUserCount = userRepository.updateActiveStatusByOrganizationId(id, activate);

        if (!activate) {
            // Only revoke tokens on deactivation — no need to touch tokens on activation
            orgUsers.forEach(refreshTokenService::revokeAllUserTokens);
            log.info("Tokens revoked for {} users in org={}", orgUsers.size(), id);
        }

        // FIX #5 — was: outletRepository.save(outlet) called in a loop → N+1 UPDATE queries.
        // Now: single bulk update query via repository method.
        // Requires: void updateActiveStatusByOrganizationId(String orgId, boolean active)
        // in OutletRepository — same pattern as userRepository bulk update.
        List<Outlet> orgOutlets = outletRepository.findByOrganizationId(id);
        outletRepository.updateActiveStatusByOrganizationId(id, activate); // FIX #5 — single bulk UPDATE

        // FIX #12 — was: null passed as organizationId in audit log.
        // The activation is scoped to this org — pass its id.
        auditService.logAction(performedBy, performedByRole, id,
                activate ? "ACTIVATE_ORGANIZATION" : "DEACTIVATE_ORGANIZATION",
                "ORGANIZATION", id,
                (activate ? "Activated" : "Deactivated") + " organization: " + org.getName()
                        + " — users affected: " + updatedUserCount
                        + ", outlets affected: " + orgOutlets.size());

        log.info("Organization {}: id={}, usersAffected={}, outletsAffected={}, by={}",
                activate ? "activated" : "deactivated", id,
                updatedUserCount, orgOutlets.size(), performedBy);

        return updatedUserCount;
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────

    private OrganizationResponse mapToResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .address(org.getAddress())
                .isActive(org.isActive())
                .createdAt(org.getCreatedAt())
                .build();
    }
}