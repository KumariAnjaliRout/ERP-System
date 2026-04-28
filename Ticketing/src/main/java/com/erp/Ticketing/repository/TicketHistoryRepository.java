package com.erp.Ticketing.repository;

import com.erp.Ticketing.model.Ticket;
import com.erp.Ticketing.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicket_IdOrderByUpdatedAtAsc(Long ticketId);
    List<TicketHistory> findByTicket_RaisedByIdOrderByUpdatedAtAsc(String raisedById);


}
