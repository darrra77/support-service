package com.example.support.report

import com.example.support.model.Priority
import com.example.support.model.Ticket
import com.example.support.model.TicketStatus
import com.example.support.repository.TicketRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportServiceTest {
    private val repository =
        Mockito.mock(TicketRepository::class.java)

    private val clock =
        Clock.fixed(
            Instant.parse("2026-11-16T12:00:00Z"),
            ZoneId.of("UTC"),
        )

    private val service = ReportService(repository, clock)

    private val from =
        LocalDateTime.of(2026, 11, 16, 0, 0)

    private val to =
        LocalDateTime.of(2026, 11, 17, 0, 0)

    @Test
    fun testEmptyReport() {
        Mockito
            .`when`(
                repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    from,
                    to,
                ),
            ).thenReturn(emptyList())

        val result = service.getSummary(from, to)

        assertEquals(0, result.created)
        assertEquals(0, result.overdue)
        assertNull(result.averageResolutionMinutes)
    }

    @Test
    fun testStatusAndPriorityCount() {
        val ticket1 = Ticket()
        ticket1.status = TicketStatus.NEW
        ticket1.priority = Priority.HIGH

        val ticket2 = Ticket()
        ticket2.status = TicketStatus.RESOLVED
        ticket2.priority = Priority.HIGH

        Mockito
            .`when`(
                repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    from,
                    to,
                ),
            ).thenReturn(listOf(ticket1, ticket2))

        val result = service.getSummary(from, to)

        assertEquals(2, result.created)
        assertEquals(1, result.byStatus["NEW"])
        assertEquals(1, result.byStatus["RESOLVED"])
        assertEquals(2, result.byPriority["HIGH"])
    }

    @Test
    fun testInvalidPeriod() {
        assertFailsWith<ResponseStatusException> {
            service.getSummary(to, from)
        }
    }

    @Test
    fun testOverdueTicket() {
        val ticket = Ticket()

        ticket.status = TicketStatus.NEW
        ticket.priority = Priority.HIGH

        ticket.dueAt =
            LocalDateTime.of(
                2026,
                11,
                15,
                12,
                0,
            )

        Mockito
            .`when`(
                repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    from,
                    to,
                ),
            ).thenReturn(listOf(ticket))

        val result = service.getSummary(from, to)

        assertEquals(1, result.overdue)
    }
}
