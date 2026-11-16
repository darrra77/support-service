package com.example.support.service;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import com.example.support.model.Ticket;
import com.example.support.model.TicketStatus;
import com.example.support.model.TicketStatusHistory;
import com.example.support.repository.CategoryRepository;
import com.example.support.repository.TicketRepository;
import com.example.support.repository.TicketStatusHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

  private final TicketRepository ticketRepository;
  private final CategoryRepository categoryRepository;
  private final TicketStatusHistoryRepository historyRepository;

  public TicketService(
      TicketRepository ticketRepository,
      CategoryRepository categoryRepository,
      TicketStatusHistoryRepository historyRepository) {

    this.ticketRepository = ticketRepository;
    this.categoryRepository = categoryRepository;
    this.historyRepository = historyRepository;
  }

  public Ticket createTicket(CreateTicketRequest request) {

    Category category =
        categoryRepository
            .findById(request.getCategoryId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Категория не найдена"));

    Ticket ticket = new Ticket();

    ticket.setTitle(request.getTitle());
    ticket.setDescription(request.getDescription());
    ticket.setCategory(category);
    ticket.setPriority(request.getPriority());
    ticket.setDueAt(request.getDueAt());

    ticket.setStatus(TicketStatus.NEW);
    ticket.setCreatedAt(LocalDateTime.now());

    return ticketRepository.save(ticket);
  }

  public Ticket getTicketById(Long id) {

    return ticketRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
  }

  @Transactional
  public Ticket updateTicket(Long id, UpdateTicketRequest request) {

    Ticket ticket = getTicketById(id);

    if (ticket.getStatus() == TicketStatus.RESOLVED
        || ticket.getStatus() == TicketStatus.CANCELLED) {

      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Закрытую заявку нельзя редактировать");
    }

    ticket.setDescription(request.getDescription());
    ticket.setPriority(request.getPriority());
    ticket.setDueAt(request.getDueAt());

    return ticketRepository.save(ticket);
  }

  @Transactional
  public Ticket changeStatus(Long id, TicketStatus newStatus) {

    Ticket ticket = getTicketById(id);

    TicketStatus oldStatus = ticket.getStatus();

    boolean allowed = false;

    if (oldStatus == TicketStatus.NEW) {
      allowed = newStatus == TicketStatus.IN_PROGRESS || newStatus == TicketStatus.CANCELLED;
    }

    if (oldStatus == TicketStatus.IN_PROGRESS) {
      allowed = newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CANCELLED;
    }

    if (!allowed) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Запрещённый переход статуса");
    }

    LocalDateTime now = LocalDateTime.now();

    ticket.setStatus(newStatus);

    if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CANCELLED) {

      ticket.setClosedAt(now);
    }

    TicketStatusHistory history = new TicketStatusHistory();

    history.setTicket(ticket);
    history.setOldStatus(oldStatus);
    history.setNewStatus(newStatus);
    history.setChangedAt(now);

    ticketRepository.save(ticket);
    historyRepository.save(history);

    return ticket;
  }

  public List<TicketStatusHistory> getStatusHistory(Long id) {

    getTicketById(id);

    return historyRepository.findByTicketIdOrderByChangedAtAsc(id);
  }

  public Page<Ticket> getTickets(
      TicketStatus status,
      Priority priority,
      Long categoryId,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size) {

    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Неверный номер страницы или размер");
    }

    if (from != null && to != null && !from.isBefore(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный период");
    }

    Specification<Ticket> filter = (root, query, cb) -> cb.conjunction();

    if (status != null) {
      filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    if (priority != null) {
      filter = filter.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
    }

    if (categoryId != null) {
      filter =
          filter.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
    }

    if (from != null) {
      filter =
          filter.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
    }

    if (to != null) {
      filter = filter.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
    }

    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    return ticketRepository.findAll(filter, pageable);
  }
}
