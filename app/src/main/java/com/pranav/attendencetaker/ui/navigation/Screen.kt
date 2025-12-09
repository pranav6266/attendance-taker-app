package com.pranav.attendencetaker.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Stats : Screen("stats")

    data object StudentList : Screen("student_list")

    // Optional argument: studentId. If null/empty, it's "Add Mode".
    data object StudentEntry : Screen("student_entry?studentId={studentId}") {
        fun createRoute(studentId: String? = null) =
            if (studentId.isNullOrEmpty()) "student_entry" else "student_entry?studentId=$studentId"
    }

    data object DayDetail : Screen("day_detail/{dateId}") {
        fun createRoute(dateId: String) = "day_detail/$dateId"
    }
}