package com.example.support.dto;

import com.example.support.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class ChangeStatusRequest {

  @NotNull private TicketStatus status;

  public TicketStatus getStatus() {
    return status;
  }

  public void setStatus(TicketStatus status) {
    this.status = status;
  }
}
