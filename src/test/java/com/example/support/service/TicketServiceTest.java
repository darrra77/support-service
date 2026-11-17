package com.example.support.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.model.*;
import com.example.support.repository.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

  @Mock private TicketRepository ticketRepository;

  @Mock private CategoryRepository categoryRepository;

  @Mock private TicketStatusHistoryRepository historyRepository;

  private TicketService ticketService;

  @BeforeEach
  void setUp() {
    ticketService = new TicketService(ticketRepository, categoryRepository, historyRepository);
  }

  // Создаём заявку для тестирования
  private Ticket createTestTicket(TicketStatus status) {

    Ticket ticket = new Ticket();

    ticket.setTitle("Не работает компьютер");
    ticket.setDescription("Компьютер не включается");
    ticket.setPriority(Priority.HIGH);
    ticket.setStatus(status);
    ticket.setCreatedAt(LocalDateTime.now());

    return ticket;
  }

  // Тест 1. Создание заявки
  @Test
  void testCreateTicket() {

    CreateTicketRequest request = new CreateTicketRequest();

    request.setTitle("Не работает интернет");
    request.setCategoryId(1L);
    request.setPriority(Priority.HIGH);

    Category category = new Category("Сеть");

    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

    when(ticketRepository.save(any(Ticket.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Ticket result = ticketService.createTicket(request);

    assertEquals("Не работает интернет", result.getTitle());
    assertEquals(TicketStatus.NEW, result.getStatus());
    assertEquals(Priority.HIGH, result.getPriority());
    assertNotNull(result.getCreatedAt());

    verify(ticketRepository).save(any(Ticket.class));
  }

  // Тест 2. Категория не существует
  @Test
  void testCreateTicketWithInvalidCategory() {

    CreateTicketRequest request = new CreateTicketRequest();

    request.setTitle("Проблема");
    request.setCategoryId(999L);
    request.setPriority(Priority.LOW);

    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> ticketService.createTicket(request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

    verify(ticketRepository, never()).save(any(Ticket.class));
  }

  // Тест 3. Получение несуществующей заявки
  @Test
  void testTicketNotFound() {

    when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> ticketService.getTicketById(999L));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  // Тест 4. Перевод новой заявки в работу
  @Test
  void testChangeStatusToInProgress() {

    Ticket ticket = createTestTicket(TicketStatus.NEW);

    when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

    Ticket result = ticketService.changeStatus(1L, TicketStatus.IN_PROGRESS);

    assertEquals(TicketStatus.IN_PROGRESS, result.getStatus());

    verify(ticketRepository).save(ticket);
    verify(historyRepository).save(any(TicketStatusHistory.class));
  }

  // Тест 5. Запрещённый переход NEW - RESOLVED
  @Test
  void testInvalidStatusTransition() {

    Ticket ticket = createTestTicket(TicketStatus.NEW);

    when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> ticketService.changeStatus(1L, TicketStatus.RESOLVED));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

    verify(historyRepository, never()).save(any(TicketStatusHistory.class));
  }

  // тест 6. Завершение заявки
  @Test
  void testResolveTicket() {

    Ticket ticket = createTestTicket(TicketStatus.IN_PROGRESS);

    when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

    Ticket result = ticketService.changeStatus(1L, TicketStatus.RESOLVED);

    assertEquals(TicketStatus.RESOLVED, result.getStatus());
    assertNotNull(result.getClosedAt());

    verify(historyRepository).save(any(TicketStatusHistory.class));
  }

  // тест 7. Запрет редактирования закрытой заявки
  @Test
  void testCannotEditClosedTicket() {

    Ticket ticket = createTestTicket(TicketStatus.RESOLVED);

    when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

    UpdateTicketRequest request = new UpdateTicketRequest();

    request.setDescription("Новое описание");
    request.setPriority(Priority.LOW);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> ticketService.updateTicket(1L, request));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

    verify(ticketRepository, never()).save(any(Ticket.class));
  }

  // тест 8. Редактирование открытой заявки
  @Test
  void testEditOpenTicket() {

    Ticket ticket = createTestTicket(TicketStatus.NEW);

    when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

    when(ticketRepository.save(any(Ticket.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateTicketRequest request = new UpdateTicketRequest();

    request.setDescription("Изменённое описание");
    request.setPriority(Priority.MEDIUM);

    Ticket result = ticketService.updateTicket(1L, request);

    assertEquals("Изменённое описание", result.getDescription());
    assertEquals(Priority.MEDIUM, result.getPriority());

    verify(ticketRepository).save(ticket);
  }

  @Test
  void testUpdatePreservesDueAtWhenOmitted() {

    Ticket ticket = createTestTicket(TicketStatus.NEW);

    LocalDateTime originalDueAt = LocalDateTime.of(2026, 12, 30, 18, 0);

    ticket.setDueAt(originalDueAt);

    when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

    when(ticketRepository.save(any(Ticket.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateTicketRequest request = new UpdateTicketRequest();

    request.setDescription("Изменённое описание");
    request.setPriority(Priority.MEDIUM);

    Ticket result = ticketService.updateTicket(1L, request);

    assertEquals(originalDueAt, result.getDueAt());
  }
}
