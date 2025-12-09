package com.pranav.attendencetaker.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pranav.attendencetaker.R
import com.pranav.attendencetaker.data.AuthRepository
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val repo = remember { AuthRepository() }

    LaunchedEffect(Unit) {
        // 1. Animate Logo and Text
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )

        // 2. Wait a bit
        delay(2000)

        // 3. Check Auth Status
        if (repo.isUserLoggedIn()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoGreen), // Duolingo Green Background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO PLACEHOLDER ---
            // Keep this blank space to attach your logo later
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale.value)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo), // <--- YOUR NEW DRAWABLE
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(180.dp) // Adjust size as needed
                        .scale(scale.value)
                    // We don't need the background modifier anymore as the logo has its own color
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- APP NAME ---
            Text(
                text = "TKD Attendance",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}