package com.pranav.attendencetaker.data.model

import com.google.firebase.firestore.Exclude
import java.util.Date

data class DailyLog(
    // 1. This is the missing line causing your error!
    // We mark it @Exclude so Firestore doesn't try to save "id" as a field inside the document duplicates.
    @get:Exclude var id: String = "",

    val date: Date = Date(),
    val focus: String = "",
    val finalized: Boolean = false,
    val attendance: Map<String, AttendanceStatus> = emptyMap()
)