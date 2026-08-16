package com.example.data.repository

import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSDateFormatter

actual fun getSyncDateRange(): Pair<String, String> {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
    }
    val now = NSDate()
    val calendar = NSCalendar.currentCalendar
    val threeMonthsAgo = calendar.dateByAddingUnit(
        value = -3,
        unit = NSCalendarUnitMonth,
        toDate = now,
        options = 0UL
    ) ?: now
    
    val toDateStr = formatter.stringFromDate(now)
    val fromDateStr = formatter.stringFromDate(threeMonthsAgo)
    return Pair(fromDateStr, toDateStr)
}

actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1990 * 1000).toLong()

