package com.pranav.attendencetaker.ui.screens.home

import com.pranav.attendencetaker.data.model.Student

data class HomeUiState(
    val isLoading: Boolean = true,
    val studentsQueue: List<Student> = emptyList(), // The cards waiting to be swiped
    val totalCount: Int = 0, // Total students (for the progress bar)
    val progress: Float = 0f, // 0.0 to 1.0 (for the Duolingo progress bar)
    val finished: Boolean = false, // If true, show the "You're done!" animation
    val error: String? = null
)