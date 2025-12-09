package com.pranav.attendencetaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
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
        FirebaseApp.initializeApp(this) // Force init (keeps GoogleApiManager happy)
        enableEdgeToEdge()

        // --- TEMPORARY: ADD DUMMY DATA BUTTON LOGIC ---
        // Uncomment this ONLY ONCE, run the app, then comment it out again.
//         seedDummyData()
        // ----------------------------------------------

        setContent {
            AttendenceTakerTheme {
                val navController = androidx.navigation.compose.rememberNavController()

                androidx.navigation.compose.NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    // 1. HOME SCREEN
                    composable(Screen.Home.route) {
                        val viewModel: HomeViewModel = viewModel()
                        HomeScreen(
                            viewModel = viewModel,
                            // You need to update HomeScreen to accept this callback
                            onNavigateToStats = { navController.navigate(Screen.Stats.route) }
                        )
                    }

                    // 2. STATS (CALENDAR) SCREEN
                    composable(Screen.Stats.route) {
                        StatsScreen(navController = navController)
                    }

                    // 3. DAY DETAIL SCREEN
                    composable(
                        route = Screen.DayDetail.route,
                        // This allows passing the "dateId" in the URL
                        arguments = listOf(androidx.navigation.navArgument("dateId") {
                            type = androidx.navigation.NavType.StringType
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