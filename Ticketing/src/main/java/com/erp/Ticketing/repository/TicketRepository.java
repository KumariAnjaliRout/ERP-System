package com.erp.Ticketing.repository;

import com.erp.Ticketing.model.Ticket;
import com.erp.Ticketing.model.TicketPriority;
import com.erp.Ticketing.model.TicketStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByRaisedById(String raisedById);

    Page<Ticket> findAll(Pageable pageable);

    Page<Ticket> findByRaisedById(String raisedById, Pageable pageable);

    Page<Ticket> findByTicketStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);

    List<Ticket> findByAdminId(String adminId);


    // changed
    List<Ticket> findByAdminIdAndRaisedByIdNot(String adminId, String raisedById);

    Page<Ticket> findByOrganizationId(String organizationId, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndTicketStatus(String organizationId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndPriority(String organizationId, TicketPriority priority, Pageable pageable);

}

