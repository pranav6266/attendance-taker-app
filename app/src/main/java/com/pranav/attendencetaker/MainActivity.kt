package com.pranav.attendencetaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.pranav.attendencetaker.data.SettingsRepository
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.screens.details.DayDetailScreen
import com.pranav.attendencetaker.ui.screens.entry.StudentEntryScreen
import com.pranav.attendencetaker.ui.screens.home.HomeScreen
import com.pranav.attendencetaker.ui.screens.home.HomeViewModel
import com.pranav.attendencetaker.ui.screens.list.StudentListScreen
import com.pranav.attendencetaker.ui.screens.login.LoginScreen
import com.pranav.attendencetaker.ui.screens.profile.ProfileScreen
import com.pranav.attendencetaker.ui.screens.splash.SplashScreen
import com.pranav.attendencetaker.ui.screens.stats.StatsScreen
import com.pranav.attendencetaker.ui.theme.AppTheme
import com.pranav.attendencetaker.ui.theme.AttendenceTakerTheme
import com.pranav.attendencetaker.worker.NotificationWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // 1. Schedule Notifications
        NotificationWorker.schedule(this)

        val settingsRepo = SettingsRepository(this)

        setContent {
            // 2. Load Theme Preference
            val appThemeState = androidx.compose.runtime.produceState(initialValue = AppTheme.SYSTEM) {
                settingsRepo.appThemeFlow.collect { value = it }
            }

            AttendenceTakerTheme(appTheme = appThemeState.value) { // Pass theme here
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    composable(Screen.Splash.route) { SplashScreen(navController) }
                    composable(Screen.Login.route) { LoginScreen(navController) }

                    composable(Screen.Home.route) {
                        // ... existing Home config ...
                        // Make sure to pass onNavigateToProfile
                        HomeScreen(
                            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                            onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                            navController = navController,
                            onNavigateToStudents = { navController.navigate(Screen.StudentList.route) },
                            onNavigateToProfile = { navController.navigate(Screen.Profile.route) } // LINKED!
                        )
                    }

                    composable(Screen.Stats.route) { StatsScreen(navController) }
                    composable(Screen.StudentList.route) { StudentListScreen(navController) }

                    // NEW PROFILE ROUTE
                    composable(Screen.Profile.route) {
                        ProfileScreen(navController = navController)
                    }

                    composable(
                        route = Screen.DayDetail.route,
                        arguments = listOf(navArgument("dateId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val dateId = backStackEntry.arguments?.getString("dateId") ?: ""
                        DayDetailScreen(dateId = dateId, navController = navController)
                    }

                    composable(
                        route = Screen.StudentEntry.route,
                        arguments = listOf(navArgument("studentId") {
                            type = NavType.StringType
                            nullable = true
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