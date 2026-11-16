package com.example.support.dto;

import com.example.support.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class CreateTicketRequest {

  @NotBlank(message = "Заголовок обязателен")
  @Size(max = 200, message = "Максимальная длина заголовка 200 символов")
  private String title;

  private String description;

  @NotNull(message = "Категория обязательна")
  private Long categoryId;

  @NotNull(message = "Приоритет обязателен")
  private Priority priority;

  private LocalDateTime dueAt;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
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
