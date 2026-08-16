package com.example.data.repository

import java.time.LocalDate
import java.time.format.DateTimeFormatter

actual fun getSyncDateRange(): Pair<String, String> {
    val toDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val fromDateStr = LocalDate.now().minusMonths(3).format(DateTimeFormatter.ISO_LOCAL_DATE)
    return Pair(fromDateStr, toDateStr)
}

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
