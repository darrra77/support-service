package com.example.support.controller;

import com.example.support.dto.ChangeStatusRequest;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.model.Priority;
import com.example.support.model.Ticket;
import com.example.support.model.TicketStatus;
import com.example.support.model.TicketStatusHistory;
import com.example.support.service.TicketService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Ticket createTicket(@Valid @RequestBody CreateTicketRequest request) {

    return ticketService.createTicket(request);
  }

  @GetMapping
  public Page<Ticket> getTickets(
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    return ticketService.getTickets(status, priority, categoryId, from, to, page, size);
  }

  @GetMapping("/{id}")
  public Ticket getTicketById(@PathVariable Long id) {

    return ticketService.getTicketById(id);
  }

  @PatchMapping("/{id}")
  public Ticket updateTicket(
      @PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {

    return ticketService.updateTicket(id, request);
  }

  @PatchMapping("/{id}/status")
  public Ticket changeStatus(
      @PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {

    return ticketService.changeStatus(id, request.getStatus());
  }

  @GetMapping("/{id}/history")
  public List<TicketStatusHistory> getHistory(@PathVariable Long id) {

    return ticketService.getStatusHistory(id);
  }
}
