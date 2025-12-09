package com.pranav.attendencetaker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CalculateStreakUseCase {

    /**
     * Calculates the new streak.
     * @param currentStreak The value currently stored in Firestore for the student.
     * @param lastAttendanceTimestamp The 'lastAttended' timestamp (Long) from Firestore.
     * @return The integer value of the NEW streak to be saved.
     */
    operator fun invoke(currentStreak: Int, lastAttendanceTimestamp: Long?): Int {
        // Case 1: Student has never attended before. Start Streak at 1.
        if (lastAttendanceTimestamp == null || lastAttendanceTimestamp == 0L) {
            return 1
        }

        // Convert the Firestore timestamp (Long) to a local Date object
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val lastDate = Instant.ofEpochMilli(lastAttendanceTimestamp)
            .atZone(zoneId)
            .toLocalDate()

        // Logic to determine the new streak
        return when {
            // Case 2: Already attended today?
            // (e.g., clicked button twice). Do NOT increment. Return existing.
            lastDate.isEqual(today) -> currentStreak

            // Case 3: Attended Yesterday?
            // The difference between today and last date is exactly 1 day. Increment!
            ChronoUnit.DAYS.between(lastDate, today) == 1L -> currentStreak + 1

            // Case 4: Missed a day (or more)?
            // Streak breaks. Reset to 1 (starting today).
            else -> 1
        }
    }
}