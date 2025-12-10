package com.pranav.attendencetaker.domain

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class CalculateStreakUseCase {

    /**
     * Calculates the streak dynamically based on the history of attended dates.
     * @param attendedDates List of all Dates the student was marked PRESENT or LATE.
     * @return The calculated streak count.
     */
    operator fun invoke(attendedDates: List<Date>): Int {
        if (attendedDates.isEmpty()) return 0

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        // 1. Convert to LocalDate, remove duplicates, and sort newest first
        val sortedDates = attendedDates
            .map { it.toInstant().atZone(zoneId).toLocalDate() }
            .distinct()
            .sortedDescending()

        var streak = 0

        // 2. Determine where to start counting
        // If they attended TODAY, we start counting from Today.
        // If they haven't attended Today yet, we check Yesterday (streak is still alive).
        var checkDate = if (sortedDates.contains(today)) {
            today
        } else {
            today.minusDays(1)
        }

        // 3. Loop backwards checking for consecutive days
        while (sortedDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }
}