package com.erp.Ticketing.controller;

import com.erp.Ticketing.config.CustomUserPrincipal;
import com.erp.Ticketing.dto.MyTicketHistoryDto;
import com.erp.Ticketing.dto.TicketRequestDto;
import com.erp.Ticketing.dto.TicketResponseDto;
import com.erp.Ticketing.model.*;
import com.erp.Ticketing.repository.TicketHistoryRepository;
import com.erp.Ticketing.repository.TicketRepository;
import com.erp.Ticketing.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.erp.Ticketing.client.AuthClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.erp.Ticketing.model.TicketHistory;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    @Autowired
    private TicketService ticketService;
    @Autowired
    private AuthClient authClient;
    @Autowired
    private TicketHistoryRepository ticketHistoryRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @PostMapping("/raise")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE', 'MANAGER', 'ACCOUNTANT', 'OUTLET', 'ADMIN', 'SUPER_ACCOUNTANT')")
    public ResponseEntity<TicketResponseDto> raiseTicket(
            @RequestBody TicketRequestDto request,
            org.springframework.security.core.Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        TicketResponseDto ticket = ticketService.createTicket(request, principal);
        log.info("Controller hit for /raise");
        log.info("Authorities inside controller: {}", authentication.getAuthorities());
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDto> getTicket(@PathVariable String id) {
        TicketResponseDto ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/stats/priority")
    public ResponseEntity<Map<String, Long>> getTicketCountByPriority() {
        Map<String, Long> stats = ticketService.getTicketCountByPriority();
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TicketResponseDto> updateStatus(@PathVariable String id,
                                                          @PathVariable TicketStatus status,
                                                          org.springframework.security.core.Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        TicketResponseDto ticket = ticketService.updateTicketStatus(id, status, principal);
        return ResponseEntity.ok(ticket);
    }
    @GetMapping("/stats/status")
    public ResponseEntity<Map<String, Long>> getTicketCountByStatus() {
        Map<String, Long> stats = ticketService.getTicketCountByStatus();
        return ResponseEntity.ok(stats);
    }
    @GetMapping
    public ResponseEntity<?> getAllTickets(

            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,

            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,

            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority

    ) {

        var tickets = ticketService.getTicketsPaginated(
                principal,
                page,
                size,
                sortBy,
                direction,
                status,
                priority
        );
        return ResponseEntity.ok(tickets);
    }
    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponseDto> escalateTicketToSuperAdmin(
            @PathVariable String id,
            org.springframework.security.core.Authentication authentication) {

        CustomUserPrincipal principal =
                (CustomUserPrincipal) authentication.getPrincipal();

        TicketResponseDto ticket =
                ticketService.escalateToSuperAdmin(id, principal);
        return ResponseEntity.ok(ticket);
    }
    @GetMapping("/admin/{adminId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponseDto>> getTicketsForAdmin(@PathVariable String adminId) {
        return ResponseEntity.ok(ticketService.getTicketsForAdmin(adminId));
    }

    @GetMapping("/super-admin/solvable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<TicketResponseDto>> getSolvableTicketsForSuperAdmin() {
        return ResponseEntity.ok(ticketService.getSolvableTicketsForSuperAdmin());
    }

    @GetMapping("/history/my")
    public ResponseEntity<List<MyTicketHistoryDto>> getMyTicketHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        List<TicketHistory> historyList =
                ticketHistoryRepository
                        .findByTicket_RaisedByIdOrderByUpdatedAtAsc(principal.getUserId());

        Map<Long, MyTicketHistoryDto> map = new LinkedHashMap<>();

        for (TicketHistory h : historyList) {

            Long ticketId = h.getTicket().getId();

            MyTicketHistoryDto dto = map.get(ticketId);

            if (dto == null) {
                dto = new MyTicketHistoryDto();
                dto.setTicketId(ticketId);
                dto.setTitle(h.getTicket().getTitle());
                dto.setDescription(h.getTicket().getDescription());
                dto.setPriority(h.getTicket().getPriority().name());
                dto.setStatus(h.getTicket().getTicketStatus().name());
                map.put(ticketId, dto);
            }

            dto.setOldStatus(
                    h.getOldStatus() == null ? "CREATED" : h.getOldStatus().name()
            );

            dto.setNewStatus(
                    h.getNewStatus() == null ? null : h.getNewStatus().name()
            );
            dto.setUpdatedByRole(h.getUpdatedByRole());
            dto.setUpdatedAt(h.getUpdatedAt());
            dto.setCreatedAt(h.getTicket().getCreatedAt());

        }

        List<MyTicketHistoryDto> response = new ArrayList<>(map.values());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuthConnection(
            org.springframework.security.core.Authentication authentication) {

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "JWT parsed successfully",
                "principal", Map.of(
                        "userId", principal.getUserId(),
                        "role", principal.getRole()
                )
        ));
    }
}

