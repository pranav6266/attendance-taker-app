package com.pranav.attendencetaker.ui.screens.login

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pranav.attendencetaker.R
import com.pranav.attendencetaker.data.AuthRepository
import com.pranav.attendencetaker.ui.components.DuoLabelButton
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }
    var isLoading by remember { mutableStateOf(false) }

    // Google Sign In Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            scope.launch {
                isLoading = true
                val success = repo.signInWithGoogle(data)
                isLoading = false
                if (success) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Sign In Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- HERO IMAGE PLACEHOLDER ---
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFF7F7F7), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo), // <--- YOUR NEW DRAWABLE
                    contentDescription = "App Logo",
                    modifier = Modifier.size(180.dp) // Adjust size to fit nicely
                ) }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Taekwondo\nAttendance",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4B4B4B),
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Manage your dojo, track students, and visualize progress securely.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isLoading) {
                CircularProgressIndicator(color = DuoBlue)
            } else {
                DuoLabelButton(
                    text = "GET STARTED",
                    color = DuoGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Launch Google Sign In Intent
                        val signInClient = repo.getGoogleSignInClient(context)
                        launcher.launch(signInClient.signInIntent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}