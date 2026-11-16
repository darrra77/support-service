package com.example.support.repository;

import com.example.support.model.TicketStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

  List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}
