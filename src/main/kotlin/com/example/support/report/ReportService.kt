package com.example.support.report

import com.example.support.model.TicketStatus
import com.example.support.repository.TicketRepository

import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Service
class ReportService(
    private val ticketRepository: TicketRepository,
    private val clock: Clock
) {

    fun getSummary(
        from: LocalDateTime,
        to: LocalDateTime
    ): ReportSummary {

        if (!from.isBefore(to)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Неверный период"
            )
        }

        val tickets = ticketRepository
            .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)

        val now = LocalDateTime.now(clock)

        // Количество созданных заявок
        val created = tickets.size

        // Распределение по статусам
        val byStatus = tickets
            .groupingBy { it.status.name }
            .eachCount()

        // Распределение по приоритетам
        val byPriority = tickets
            .groupingBy { it.priority.name }
            .eachCount()

        // Просроченные открытые заявки
        val overdue = tickets.count { ticket ->

            val isOpen =
                ticket.status == TicketStatus.NEW ||
                        ticket.status == TicketStatus.IN_PROGRESS

            val isOverdue =
                ticket.dueAt?.isBefore(now) == true

            isOpen && isOverdue
        }

        // Только решённые заявки
        val resolved = tickets.filter {
            it.status == TicketStatus.RESOLVED &&
                    it.closedAt != null
        }

        // Среднее время решения
        val averageResolutionMinutes =
            resolved
                .mapNotNull { ticket ->

                    ticket.closedAt?.let { closed ->

                        Duration.between(
                            ticket.createdAt,
                            closed
                        ).toMillis() / 60000.0
                    }
                }
                .average()
                .takeIf { !it.isNaN() }

        return ReportSummary(
            created = created,
            byStatus = byStatus,
            byPriority = byPriority,
            overdue = overdue,
            averageResolutionMinutes = averageResolutionMinutes
        )
    }
}