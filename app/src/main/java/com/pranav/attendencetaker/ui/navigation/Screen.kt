package com.pranav.attendencetaker.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Stats : Screen("stats")
    data object DayDetail : Screen("day_detail/{dateId}") {
        fun createRoute(dateId: String) = "day_detail/$dateId"
    }
}