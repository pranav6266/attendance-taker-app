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
import com.pranav.attendencetaker.ui.screens.home.HomeScreen
import com.pranav.attendencetaker.ui.screens.home.HomeViewModel
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
                            navController = navController // <--- PASSED HERE
                        )
                    }

                    // 2. STATS (CALENDAR) SCREEN
                    composable(Screen.Stats.route) {
                        StatsScreen(navController = navController)
                    }

                    // 3. DAY DETAIL SCREEN
                    composable(
                        route = Screen.DayDetail.route,
                        arguments = listOf(navArgument("dateId") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        val dateId = backStackEntry.arguments?.getString("dateId") ?: ""
                        DayDetailScreen(dateId = dateId, navController = navController)
                    }
                }
            }
        }
    }
}