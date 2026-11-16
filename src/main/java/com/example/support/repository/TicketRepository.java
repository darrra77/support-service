package com.example.support.repository;

import com.example.support.model.Ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository
        extends JpaRepository<Ticket, Long>,
        JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime from,
            LocalDateTime to
    );
}