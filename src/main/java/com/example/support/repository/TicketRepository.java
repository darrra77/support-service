package com.example.support.repository;

import com.example.support.model.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository
    extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

  List<Ticket> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      LocalDateTime from, LocalDateTime to);
}
