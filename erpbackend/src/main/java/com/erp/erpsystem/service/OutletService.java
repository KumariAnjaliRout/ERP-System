package com.erp.erpsystem.service;

import com.erp.erpsystem.dto.CreateOutletRequest;
import com.erp.erpsystem.dto.OutletResponse;
import com.erp.erpsystem.dto.UpdateOutletRequest;
import com.erp.erpsystem.entity.Outlet;
import com.erp.erpsystem.entity.Organization;
import com.erp.erpsystem.exception.BadRequestException;
import com.erp.erpsystem.exception.DuplicateResourceException;
import com.erp.erpsystem.exception.ForbiddenException;
import com.erp.erpsystem.exception.ResourceNotFoundException;
import com.erp.erpsystem.repository.OrganizationRepository;
import com.erp.erpsystem.repository.OutletRepository;
import com.erp.erpsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutletService {

    private final OutletRepository outletRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public OutletResponse createOutlet(CreateOutletRequest request, String organizationId,
                                       UUID adminUserId, String userRole) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization '" + organizationId + "' does not exist"));

        if (!org.isActive()) {
            throw new BadRequestException(
                    "Cannot create outlet under inactive organization '" + organizationId + "'");
        }

        // FIX #7 — normalize outlet ID: trim + uppercase to prevent silent duplicates
        // e.g. "out001" and "OUT001 " resolving to different records
        String normalizedId = request.getId().trim().toUpperCase();

        if (outletRepository.existsById(normalizedId)) {
            throw new DuplicateResourceException(
                    "Outlet with ID '" + normalizedId + "' already exists");
        }

        Outlet outlet = Outlet.builder()
                .id(normalizedId)                   // FIX #7 — use normalized ID
                .name(request.getName().trim())     // trim name for consistency
                .organizationId(organizationId)
                .address(request.getAddress())
                .build();

        Outlet savedOutlet = outletRepository.save(outlet);

        auditService.logCreate(adminUserId, userRole, organizationId,
                "OUTLET", savedOutlet.getId(), savedOutlet.getName());

        log.info("Outlet created: id={}, orgId={}, by={}", savedOutlet.getId(), organizationId, adminUserId);
        return mapToResponse(savedOutlet);
    }

    // ── GET ALL (Paginated) ───────────────────────────────────────────────────

    public Page<OutletResponse> getAllOutletsPaginated(Pageable pageable) {
        return outletRepository.findAll(pageable).map(this::mapToResponse);
    }

    // ── GET BY ORGANIZATION (Paginated) ───────────────────────────────────────

    public Page<OutletResponse> getOutletsByOrganizationPaginated(
            String organizationId, Pageable pageable) {
        return outletRepository.findByOrganizationIdNative(organizationId, pageable)
                .map(this::mapToResponse);
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    /**
     * FIX #3 — added callerRole and callerOrgId parameters so authorization
     * is enforced inside the service, preventing unauthorized DB reads.
     * Was: controller fetched the outlet first, then checked auth after.
     */
    public OutletResponse getOutletById(String outletId, String callerRole, String callerOrgId) {
        Outlet outlet = outletRepository.findById(outletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Outlet '" + outletId + "' not found"));

        // Authorization check co-located with the fetch — no data exposed before auth
        if (!"SUPER_ADMIN".equals(callerRole) &&
                !outlet.getOrganizationId().equals(callerOrgId)) {
            log.warn("Caller from org '{}' attempted to access outlet '{}' of org '{}'",
                    callerOrgId, outletId, outlet.getOrganizationId());
            throw new ForbiddenException("Access denied to outlet in another organization");
        }

        return mapToResponse(outlet);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public OutletResponse updateOutlet(String outletId, UpdateOutletRequest request,
                                       UUID adminUserId, String userRole,
                                       String callerOrgId) {

        Outlet outlet = outletRepository.findById(outletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Outlet '" + outletId + "' not found"));

        if (!"SUPER_ADMIN".equals(userRole) &&
                !outlet.getOrganizationId().equals(callerOrgId)) {
            throw new ForbiddenException(
                    "Cannot update outlet belonging to another organization");
        }

        StringBuilder changes = new StringBuilder();

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equals(outlet.getName())) {
                changes.append("name: '").append(outlet.getName())
                        .append("' → '").append(newName).append("'; ");
                outlet.updateName(newName);
            }
        }

        if (request.getAddress() != null) {
            // FIX #5 — was: "address updated;" with no old value.
            // Now: logs old → new address for meaningful audit trail.
            changes.append("address: '").append(outlet.getAddress())
                    .append("' → '").append(request.getAddress()).append("'; ");
            outlet.updateAddress(request.getAddress());
        }

        if (changes.isEmpty()) {
            throw new BadRequestException(
                    "No updatable fields provided. Supply at least 'name' or 'address'.");
        }

        Outlet updatedOutlet = outletRepository.save(outlet);

        auditService.logUpdate(adminUserId, userRole,
                outlet.getOrganizationId(),     // use outlet's own orgId, not callerOrgId
                "OUTLET", outletId, updatedOutlet.getName(), changes.toString());

        log.info("Outlet updated: id={}, changes={}, by={}", outletId, changes, adminUserId);
        return mapToResponse(updatedOutlet);
    }

    // ── TOGGLE ACTIVATION ─────────────────────────────────────────────────────

    @Transactional
    public void toggleOutletActivation(String outletId, boolean activate,
                                       UUID adminUserId, String userRole,
                                       String callerOrgId) {

        Outlet outlet = outletRepository.findById(outletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Outlet '" + outletId + "' not found"));

        if (!"SUPER_ADMIN".equals(userRole) &&
                !outlet.getOrganizationId().equals(callerOrgId)) {
            throw new ForbiddenException(
                    "Cannot toggle activation of outlet belonging to another organization");
        }

        // FIX #4 — guard against no-op: if already in the desired state, reject.
        // Prevents pointless DB writes and misleading audit entries.
        if (outlet.getIsActive() == activate) {
            throw new BadRequestException(
                    "Outlet is already " + (activate ? "active" : "inactive"));
        }

        if (activate) {
            Organization org = organizationRepository.findById(outlet.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Organization '" + outlet.getOrganizationId() + "' does not exist"));
            if (!org.isActive()) {
                throw new BadRequestException(
                        "Cannot activate outlet. Organization '"
                                + outlet.getOrganizationId()
                                + "' is deactivated. Reactivate the organization first.");
            }
        }

        outlet.toggleActive(activate);
        outletRepository.save(outlet);

        // FIX #10 — was: logUpdate with activation message — semantically wrong.
        // Now: logAction with dedicated ACTIVATE_OUTLET / DEACTIVATE_OUTLET action.
        auditService.logAction(adminUserId, userRole, outlet.getOrganizationId(),
                activate ? "ACTIVATE_OUTLET" : "DEACTIVATE_OUTLET",
                "OUTLET", outletId,
                (activate ? "Activated" : "Deactivated") + " outlet: " + outlet.getName());

        log.info("Outlet {}: id={}, by={}", activate ? "activated" : "deactivated", outletId, adminUserId);
    }

    // ── GET ALL (Unpaginated — for internal/microservice use only) ────────────────

    public List<OutletResponse> getAllOutlets() {
        return outletRepository.findAll(Sort.by("id").ascending())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

// ── GET BY ORGANIZATION (Unpaginated — for internal/microservice use only) ────

    public List<OutletResponse> getOutletsByOrganization(String organizationId) {
        return outletRepository.findByOrganizationId(organizationId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ── DELETE (Soft Delete) ──────────────────────────────────────────────────

    /**
     * FIX #6  — all mutations are within @Transactional so if outletRepository.save()
     *           fails after owner was already saved, the whole transaction rolls back.
     * FIX #13 — uses logAction("SOFT_DELETE_OUTLET") instead of logDelete()
     *           since the outlet is NOT actually deleted — just deactivated.
     */
    @Transactional
    public void deleteOutlet(String outletId, UUID adminUserId, String userRole,
                             String callerOrgId) {

        Outlet outlet = outletRepository.findById(outletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Outlet '" + outletId + "' not found"));

        if (!"SUPER_ADMIN".equals(userRole) &&
                !outlet.getOrganizationId().equals(callerOrgId)) {
            throw new ForbiddenException(
                    "Cannot delete outlet belonging to another organization");
        }

        // FIX #4 — guard: if already inactive, soft-delete is a no-op
        if (!outlet.getIsActive()) {
            throw new BadRequestException("Outlet is already inactive/deleted");
        }

        // Revoke outlet owner's access if one is assigned
        if (outlet.getOutletOwnerId() != null) {
            userRepository.findById(outlet.getOutletOwnerId()).ifPresentOrElse(
                    owner -> {
                        owner.removeFromOutlet();
                        userRepository.save(owner);
                        refreshTokenService.revokeAllUserTokens(owner);
                        log.info("Revoked tokens and removed outlet assignment for userId='{}'",
                                owner.getId());
                    },
                    () -> log.warn(
                            "Outlet '{}' references outletOwnerId '{}' but no matching user found. " +
                                    "Skipping token revocation.",
                            outletId, outlet.getOutletOwnerId())
            );
        }

        outlet.toggleActive(false);
        outletRepository.save(outlet);

        // FIX #13 — was: logDelete() → creates "DELETE_OUTLET" audit entry which is
        // misleading since the record still exists in DB (soft delete only).
        // Now: logAction with "SOFT_DELETE_OUTLET" accurately reflects what happened.
        auditService.logAction(adminUserId, userRole, outlet.getOrganizationId(),
                "SOFT_DELETE_OUTLET",
                "OUTLET", outletId,
                "Soft-deleted (deactivated) outlet: " + outlet.getName());

        log.info("Outlet soft-deleted: id={}, by={}", outletId, adminUserId);
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────

    private OutletResponse mapToResponse(Outlet outlet) {
        return OutletResponse.builder()
                .id(outlet.getId())
                .name(outlet.getName())
                .organizationId(outlet.getOrganizationId())
                .outletOwnerId(outlet.getOutletOwnerId())
                .isActive(outlet.getIsActive())
                .address(outlet.getAddress())
                .build();
    }
}