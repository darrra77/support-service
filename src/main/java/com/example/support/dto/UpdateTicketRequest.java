package com.example.support.dto;

import com.example.support.model.Priority;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class UpdateTicketRequest {

    private String description;

    @NotNull
    private Priority priority;

    private LocalDateTime dueAt;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
}
