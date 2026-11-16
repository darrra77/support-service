package com.example.support.report

data class ReportSummary(
    val created: Int,
    val byStatus: Map<String, Int>,
    val byPriority: Map<String, Int>,
    val overdue: Int,
    val averageResolutionMinutes: Double?
)