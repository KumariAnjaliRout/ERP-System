package com.erp.Ticketing.service;

import com.erp.Ticketing.config.CustomUserPrincipal;
import com.erp.Ticketing.client.NotificationFeignClient;
import com.erp.Ticketing.dto.NotificationRequestDto;
import com.erp.Ticketing.model.*;
import com.erp.Ticketing.repository.TicketHistoryRepository;
import com.erp.Ticketing.repository.TicketRepository;
import com.erp.Ticketing.dto.TicketRequestDto;
import com.erp.Ticketing.dto.TicketResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final NotificationFeignClient notificationFeignClient;
    @Transactional
    public TicketResponseDto createTicket(TicketRequestDto request, CustomUserPrincipal principal) {

        Ticket ticket = new Ticket();

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setRaisedById(principal.getUserId());
        ticket.setRaisedByRole(principal.getRole());

        String role = principal.getRole();

        if ("ROLE_SUPER_ACCOUNTANT".equals(role)) {

            ticket.setOrganizationId(null);
            ticket.setOutletId(null);
            ticket.setAssignedTo("ROLE_SUPER_ADMIN");
            ticket.setAdminId(null);

        } else if ("ROLE_ADMIN".equals(role)) {

            // Admin tickets go to Super Admin
            ticket.setOrganizationId(principal.getOrganizationId());
            ticket.setOutletId(principal.getOutletId());
            ticket.setAssignedTo("ROLE_SUPER_ADMIN");
            ticket.setAdminId(principal.getUserId());

        } else {

            // Outlet / Manager tickets go to Admin
            ticket.setOrganizationId(principal.getOrganizationId());
            ticket.setOutletId(principal.getOutletId());
            ticket.setAssignedTo("ROLE_ADMIN");
            ticket.setAdminId(request.getAdminId());
        }

        ticket.setTicketStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        System.out.println("Saved Ticket ID = " + saved.getId());
        TicketHistory history = new TicketHistory();
        history.setTicket(saved);
        history.setOldStatus(null);
        history.setNewStatus(saved.getTicketStatus());
        history.setUpdatedByRole(principal.getRole());
        history.setUpdatedAt(LocalDateTime.now());

        ticketHistoryRepository.save(history);
//
        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.TICKET)
                        .type(NotificationType.TICKET_CREATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(saved.getOrganizationId())
                        .outletId(saved.getOutletId())
                        .metadata(Map.of(
                            "ticketId", saved.getId(),
                            "status", saved.getTicketStatus().name(),
                            "triggeredByRole", saved.getRaisedByRole()
                        ))
                        .actionable(true)
                        .build();

    try {
        notificationFeignClient.sendNotification(notification);
    } catch (Exception e) {
        log.error("Notification failed, but ticket saved", e);
    }

        return toResponseDto(saved);
    }
    private TicketResponseDto toResponseDto(Ticket ticket) {
        TicketResponseDto dto = new TicketResponseDto();
        dto.setId(String.valueOf(ticket.getId()));
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setRaisedByRole(ticket.getRaisedByRole());
        dto.setRaisedById(ticket.getRaisedById());
        dto.setOutletId(ticket.getOutletId());
        dto.setOrganizationId(ticket.getOrganizationId());
        dto.setAdminId(ticket.getAdminId());
        dto.setPriority(ticket.getPriority());
        dto.setTicketStatus(ticket.getTicketStatus());
        dto.setAssignedTo(ticket.getAssignedTo());
        dto.setEscalatedTo(ticket.getEscalatedTo());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        return dto;
    }

    public List<TicketResponseDto> getTicketsForUser(CustomUserPrincipal principal) {

        String role = principal.getRole();

        if ("ROLE_SUPER_ADMIN".equals(role)) {

            return ticketRepository.findAll()
                    .stream()
                    .map(this::toResponseDto)
                    .toList();
        }

        if ("ROLE_ADMIN".equals(role)) {

            return ticketRepository.findByAdminId(principal.getUserId())
                    .stream()
                    .map(this::toResponseDto)
                    .toList();
        }

        return ticketRepository.findByRaisedById(principal.getUserId())
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public Map<String, Long> getTicketCountByPriority() {
        return ticketRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));
    }

    public Map<String, Long> getTicketCountByStatus() {
        return ticketRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getTicketStatus().name(), Collectors.counting()));
    }

    public TicketResponseDto getTicketById(String idStr) {
        Long id = Long.parseLong(idStr.trim());
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + idStr));
        return toResponseDto(ticket);
    }

    @Transactional
    public TicketResponseDto updateTicketStatus(String ticketIdStr,
                                                TicketStatus newStatus,
                                                CustomUserPrincipal principal) {

        Long ticketId = Long.parseLong(ticketIdStr.trim());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        TicketStatus oldStatus = ticket.getTicketStatus();

        String role = principal.getRole().startsWith("ROLE_")
                ? principal.getRole()
                : "ROLE_" + principal.getRole();

        if ("ROLE_SUPER_ACCOUNTANT".equals(role)) {
            throw new RuntimeException("Super Accountant cannot update own tickets. Only Super Admin can resolve.");
        }
        if ("ROLE_ADMIN".equals(role) &&
                ticket.getRaisedById().equals(principal.getUserId())) {

            throw new RuntimeException(
                    "Admin cannot update his own raised ticket. Only Super Admin can resolve it.");
        }

        if ("ROLE_ADMIN".equals(role)) {

            if (!"ROLE_ADMIN".equals(ticket.getAssignedTo()))
                throw new RuntimeException("Admin can update only tickets assigned to Admin");

        } else if ("ROLE_SUPER_ADMIN".equals(role)) {

            boolean assignedToSuperAdmin =
                    "ROLE_SUPER_ADMIN".equals(ticket.getAssignedTo());

            boolean raisedByAdmin =
                    "ROLE_ADMIN".equals(ticket.getRaisedByRole());

            if (!(assignedToSuperAdmin || raisedByAdmin))
                throw new RuntimeException("SuperAdmin can update only escalated or admin tickets");

        } else {
            throw new RuntimeException("Unauthorized");
        }

        System.out.println("Updating Ticket ID = " + ticket.getId());

        // UPDATE EXISTING ENTITY (NO NEW ROW)
        ticket.setTicketStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);

        //  CREATE HISTORY ENTRY
        TicketHistory history = new TicketHistory();
        history.setTicket(savedTicket);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setUpdatedByRole(principal.getRole());
        history.setUpdatedAt(LocalDateTime.now());

        ticketHistoryRepository.save(history);
        //calling notification
        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.TICKET)
                        .type(NotificationType.TICKET_STATUS_UPDATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(savedTicket.getOrganizationId())
                        .outletId(savedTicket.getOutletId())
                        .targetUserId(UUID.fromString(savedTicket.getRaisedById()))
                        .targetRole(savedTicket.getRaisedByRole().replace("ROLE_", ""))
                        .metadata(Map.of(
                                "ticketId", savedTicket.getId(),
                                "status", newStatus.name()
                        ))
                        .actionable(true)
                        .build();

        notificationFeignClient.sendNotification(notification);
        return toResponseDto(savedTicket);
    }

    public TicketResponseDto escalateToSuperAdmin(String ticketIdStr,  CustomUserPrincipal principal) {
        Long ticketId = Long.parseLong(ticketIdStr.trim());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));


        // only ADMIN can escalate
        if (!"ROLE_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Only ADMIN can escalate tickets");
        }

        // only escalate tickets assigned to admin
        if (!"ROLE_ADMIN".equals(ticket.getAssignedTo())) {
            throw new RuntimeException("Only ADMIN assigned tickets can be escalated");
        }

        TicketStatus oldStatus = ticket.getTicketStatus();
        ticket.setAssignedTo("ROLE_SUPER_ADMIN");
        ticket.setEscalatedTo("ROLE_SUPER_ADMIN");
        ticket.setEscalated(true);
        ticket.setTicketStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

//        TicketStatus oldStatus = ticket.getTicketStatus();
        TicketHistory history = new TicketHistory();
        history.setTicket(saved);
        history.setOldStatus(oldStatus);
        history.setNewStatus(saved.getTicketStatus());
        history.setUpdatedByRole(principal.getRole());
        history.setUpdatedAt(LocalDateTime.now());
        ticketHistoryRepository.save(history);

        //calling notification
        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.TICKET)
                        .type(NotificationType.TICKET_ESCALATED_TO_SUPER_ADMIN)
                        .priority(NotificationPriority.HIGH)
                        .organizationId(saved.getOrganizationId())
                        .outletId(saved.getOutletId())
                        .targetRole("SUPER_ADMIN")
                        .metadata(Map.of(
                                "ticketId", saved.getId(),
                                "status", saved.getTicketStatus().name(),
                                "escalated", true
                        ))
                        .actionable(true)
                        .build();

        notificationFeignClient.sendNotification(notification);

        return toResponseDto(saved);
    }
    public List<TicketResponseDto> getTicketsForAdmin(String adminIdStr) {
        return ticketRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TicketResponseDto> getSolvableTicketsForSuperAdmin() {
        return ticketRepository.findAll().stream()
                .filter(t -> "ROLE_SUPER_ADMIN".equalsIgnoreCase(t.getAssignedTo()))
                .filter(t -> "ROLE_ADMIN".equalsIgnoreCase(t.getRaisedByRole()) ||
                        "ROLE_SUPER_ADMIN".equalsIgnoreCase(t.getEscalatedTo())||
                        "ROLE_SUPER_ACCOUNTANT".equalsIgnoreCase(t.getRaisedByRole()))
                .filter(t -> "ROLE_SUPER_ADMIN".equalsIgnoreCase(t.getAssignedTo()))
                .map(this::toResponseDto)
                .toList();
    }

    public Page<TicketResponseDto> getTicketsPaginated(
            CustomUserPrincipal principal,
            int page,
            int size,
            String sortBy,
            String direction,
            TicketStatus status,
            TicketPriority priority) {

        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Ticket> ticketPage;
        String role = principal.getRole();

        if ("ROLE_ADMIN".equals(role)) {

            if (status != null) {
                ticketPage = ticketRepository.findByOrganizationIdAndTicketStatus(
                        principal.getOrganizationId(), status, pageable);

            } else if (priority != null) {
                ticketPage = ticketRepository.findByOrganizationIdAndPriority(
                        principal.getOrganizationId(), priority, pageable);

            } else {
                ticketPage = ticketRepository.findByOrganizationId(
                        principal.getOrganizationId(), pageable);
            }

            List<Ticket> filtered = ticketPage.getContent()
                    .stream()
                    .filter(t -> !t.getRaisedById().equals(principal.getUserId()))
                    .toList();

            ticketPage = new org.springframework.data.domain.PageImpl<>(
                    filtered,
                    pageable,
                    filtered.size()
            );

        } else if ("ROLE_SUPER_ADMIN".equals(role)) {

            if (status != null) {
                ticketPage = ticketRepository.findByTicketStatus(status, pageable);

            } else if (priority != null) {
                ticketPage = ticketRepository.findByPriority(priority, pageable);

            } else {
                ticketPage = ticketRepository.findAll(pageable);
            }

        } else {
            ticketPage = ticketRepository.findByRaisedById(
                    principal.getUserId(), pageable);
        }

        return ticketPage.map(this::toResponseDto);
    }


}
