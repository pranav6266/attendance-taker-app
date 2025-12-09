package com.pranav.attendencetaker.ui.screens.home

import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student

data class HomeUiState(
    val isLoading: Boolean = true,
    val studentsQueue: List<Student> = emptyList(),
    val totalCount: Int = 0,
    val progress: Float = 0f,
    val error: String? = null,

    // New fields for logic update
    val isAttendanceFinalized: Boolean = false, // True ONLY if finalized in DB
    val dailyLog: DailyLog? = null // Holds current counts for the summary card
)