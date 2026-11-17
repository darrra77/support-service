package com.example.support.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TicketIntegrationTest {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {

    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanDatabase() {

    jdbcTemplate.execute("DELETE FROM ticket_status_history");
    jdbcTemplate.execute("DELETE FROM tickets");
  }

  // Вспомогательный метод создания заявки
  private int createTicket() throws Exception {

    String json =
        """
        {
          "title": "Не работает компьютер",
          "description": "Компьютер не включается",
          "categoryId": 1,
          "priority": "HIGH"
        }
        """;

    String response =
        mockMvc
            .perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return JsonPath.read(response, "$.id");
  }

  // Тест 1. Получение категорий
  @Test
  void testGetCategories() throws Exception {

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
  }

  // Тест 2. Создание заявки
  @Test
  void testCreateTicket() throws Exception {

    int id = createTicket();

    mockMvc
        .perform(get("/api/tickets/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Не работает компьютер"))
        .andExpect(jsonPath("$.status").value("NEW"));
  }

  // Тест 3. Несуществующая заявка
  @Test
  void testTicketNotFound() throws Exception {

    mockMvc.perform(get("/api/tickets/999999")).andExpect(status().isNotFound());
  }

  // Тест 4. Проверка обязательного заголовка
  @Test
  void testInvalidTicket() throws Exception {

    String json =
        """
        {
          "title": "",
          "categoryId": 1,
          "priority": "HIGH"
        }
        """;

    mockMvc
        .perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
  }

  // тест 5. Сохранение истории статусов
  @Test
  void testStatusHistory() throws Exception {

    int id = createTicket();

    String json =
        """
        {
          "status": "IN_PROGRESS"
        }
        """;

    mockMvc
        .perform(
            patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/tickets/" + id + "/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].oldStatus").value("NEW"))
        .andExpect(jsonPath("$[0].newStatus").value("IN_PROGRESS"));
  }

  // Тест 6. Фильтрация заявок
  @Test
  void testFilterTickets() throws Exception {

    createTicket();

    mockMvc
        .perform(get("/api/tickets").param("status", "NEW").param("priority", "HIGH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("NEW"));
  }

  // Регрессионный тест BUG-001
  @Test
  void testPatchPreservesDueAtWhenOmitted() throws Exception {

    int id = createTicket();

    // Устанавливаем срок выполнения
    mockMvc
        .perform(
            patch("/api/tickets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "description": "Описание со сроком",
                      "priority": "HIGH",
                      "dueAt": "2026-12-30T18:00:00"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dueAt").value("2026-12-30T18:00:00"));

    // Изменяем описание и приоритет, не передавая dueAt
    mockMvc
        .perform(
            patch("/api/tickets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "description": "Обновлённое описание",
                      "priority": "MEDIUM"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dueAt").value("2026-12-30T18:00:00"));

    // Проверяем сохранённые данные
    mockMvc
        .perform(get("/api/tickets/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Обновлённое описание"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.dueAt").value("2026-12-30T18:00:00"));
  }

  // Тест 7. Полный жизненный цикл заявки
  @Test
  void testFullTicketLifecycle() throws Exception {

    int id = createTicket();

    // Переводим заявку в работу
    mockMvc
        .perform(
            patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    // Редактируем заявку
    mockMvc
        .perform(
            patch("/api/tickets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "description": "Заменили кабель",
                      "priority": "MEDIUM",
                      "dueAt": "2026-11-30T18:00:00"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.priority").value("MEDIUM"));

    // Завершаем заявку
    mockMvc
        .perform(
            patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESOLVED"))
        .andExpect(jsonPath("$.closedAt").isNotEmpty());

    // Проверяем сохранённые данные
    mockMvc
        .perform(get("/api/tickets/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Заменили кабель"))
        .andExpect(jsonPath("$.status").value("RESOLVED"));

    // Проверяем историю изменений
    mockMvc
        .perform(get("/api/tickets/" + id + "/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].newStatus").value("IN_PROGRESS"))
        .andExpect(jsonPath("$[1].newStatus").value("RESOLVED"));
  }

  // Тест 8. Запрет повторного открытия отменённой заявки
  @Test
  void testCannotReopenCancelledTicket() throws Exception {

    int id = createTicket();

    // Отменяем заявку
    mockMvc
        .perform(
            patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    // Пробуем повторно открыть
    mockMvc
        .perform(
            patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isConflict());

    // Проверяем, что статус не изменился
    mockMvc
        .perform(get("/api/tickets/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    // Проверяем, что лишняя запись в истории не появилась
    mockMvc
        .perform(get("/api/tickets/" + id + "/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].oldStatus").value("NEW"))
        .andExpect(jsonPath("$[0].newStatus").value("CANCELLED"));
  }
}
