package com.pranav.attendencetaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.screens.details.DayDetailScreen
import com.pranav.attendencetaker.ui.screens.entry.StudentEntryScreen
import com.pranav.attendencetaker.ui.screens.home.HomeScreen
import com.pranav.attendencetaker.ui.screens.home.HomeViewModel
import com.pranav.attendencetaker.ui.screens.list.StudentListScreen
import com.pranav.attendencetaker.ui.screens.stats.StatsScreen
import com.pranav.attendencetaker.ui.theme.AttendenceTakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            AttendenceTakerTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    // 1. HOME SCREEN
                    composable(Screen.Home.route) {
                        val viewModel: HomeViewModel = viewModel()
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                            navController = navController,
                            onNavigateToStudents = { navController.navigate(Screen.StudentList.route) },
                            onNavigateToProfile = { /* Future Profile Screen */ }
                        )
                    }

                    // 2. STATS SCREEN
                    composable(Screen.Stats.route) {
                        StatsScreen(navController = navController)
                    }

                    // 3. DAY DETAIL SCREEN
                    composable(
                        route = Screen.DayDetail.route,
                        arguments = listOf(navArgument("dateId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val dateId = backStackEntry.arguments?.getString("dateId") ?: ""
                        DayDetailScreen(dateId = dateId, navController = navController)
                    }

                    // 4. STUDENT LIST SCREEN
                    composable(Screen.StudentList.route) {
                        StudentListScreen(navController = navController)
                    }

                    // 5. STUDENT ENTRY (ADD/EDIT) SCREEN
                    composable(
                        route = Screen.StudentEntry.route,
                        arguments = listOf(navArgument("studentId") {
                            type = NavType.StringType
                            nullable = true // Null means "Add Mode"
                        })
                    ) { backStackEntry ->
                        val studentId = backStackEntry.arguments?.getString("studentId")
                        StudentEntryScreen(studentId = studentId, navController = navController)
                    }
                }
            }
        }
    }
}