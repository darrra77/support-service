package com.example.support.service

import com.example.support.model.Ticket
import com.example.support.model.TicketStatus
import com.example.support.repository.CategoryRepository
import com.example.support.repository.TicketRepository
import com.example.support.repository.TicketStatusHistoryRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional
import kotlin.test.assertEquals

class JavaServiceTest {
    @Test
    fun testGetTicketFromJavaService() {
        // Создаём поддельные репозитории
        val ticketRepository =
            Mockito.mock(TicketRepository::class.java)

        val categoryRepository =
            Mockito.mock(CategoryRepository::class.java)

        val historyRepository =
            Mockito.mock(TicketStatusHistoryRepository::class.java)

        // Создаём Java сервис
        val service =
            TicketService(
                ticketRepository,
                categoryRepository,
                historyRepository,
            )

        // Создаём тестовую заявку
        val ticket = Ticket()
        ticket.title = "Тестовая заявка"
        ticket.status = TicketStatus.NEW

        // Настраиваем ответ репозитория
        Mockito
            .`when`(
                ticketRepository.findById(1L),
            ).thenReturn(Optional.of(ticket))

        // Вызываем Java метод из Kotlin
        val result = service.getTicketById(1L)

        // Проверяем результат
        assertEquals("Тестовая заявка", result.title)
        assertEquals(TicketStatus.NEW, result.status)
    }
}
