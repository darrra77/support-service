package com.example.support.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_status_history")
public class TicketStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status")
  private TicketStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false)
  private TicketStatus newStatus;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  public TicketStatusHistory() {}

  public Long getId() {
    return id;
  }

  public Ticket getTicket() {
    return ticket;
  }

  public void setTicket(Ticket ticket) {
    this.ticket = ticket;
  }

  public TicketStatus getOldStatus() {
    return oldStatus;
  }

  public void setOldStatus(TicketStatus oldStatus) {
    this.oldStatus = oldStatus;
  }

  public TicketStatus getNewStatus() {
    return newStatus;
  }

  public void setNewStatus(TicketStatus newStatus) {
    this.newStatus = newStatus;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(LocalDateTime changedAt) {
    this.changedAt = changedAt;
  }
}
