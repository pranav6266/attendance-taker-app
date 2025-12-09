package com.pranav.attendencetaker.ui.screens.profile

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pranav.attendencetaker.ui.components.DuoIconButton
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.AppTheme
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoRed

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val profilePicUrl by viewModel.profilePicUrl.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val morningEnabled by viewModel.morningReminder.collectAsStateWithLifecycle()
    val eveningEnabled by viewModel.eveningReminder.collectAsStateWithLifecycle()

    // Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op, just requested */ }

    // Image Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadProfileImage(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DuoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    color = Color.LightGray,
                    onClick = { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("PROFILE", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- PROFILE PICTURE ---
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (profilePicUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profilePicUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(4.dp, DuoBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .border(4.dp, DuoBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
                    }
                }

                // Edit/Camera Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, Color.LightGray, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Edit", tint = DuoBlue, modifier = Modifier.size(20.dp))
                }
            }

            Text(userEmail, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)

            Spacer(modifier = Modifier.height(32.dp))

            // --- NOTIFICATIONS ---
            SectionHeader("NOTIFICATIONS")
            SettingsCard {
                SwitchRow(
                    label = "Morning Reminder (6:15 AM)",
                    checked = morningEnabled,
                    onCheckedChange = { viewModel.toggleMorningReminder(it) }
                )
                Divider(color = MaterialTheme.colorScheme.background)
                SwitchRow(
                    label = "Evening Reminder (7:00 PM)",
                    checked = eveningEnabled,
                    onCheckedChange = { viewModel.toggleEveningReminder(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- THEME ---
            SectionHeader("APPEARANCE")
            SettingsCard {
                ThemeOptionRow("System Default", AppTheme.SYSTEM, currentTheme) { viewModel.updateTheme(it) }
                ThemeOptionRow("Light Mode", AppTheme.LIGHT, currentTheme) { viewModel.updateTheme(it) }
                ThemeOptionRow("Dark Mode", AppTheme.DARK, currentTheme) { viewModel.updateTheme(it) }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- LOGOUT ---
            Button(
                onClick = {
                    viewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) // Clear backstack
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DuoRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("LOG OUT", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
            }
        }
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = DuoBlue, checkedTrackColor = DuoBlue.copy(alpha=0.2f))
        )
    }
}

@Composable
fun ThemeOptionRow(text: String, theme: AppTheme, currentTheme: AppTheme, onSelect: (AppTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(theme) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (theme == AppTheme.DARK) Icons.Default.DarkMode else Icons.Default.Notifications, // Just generic icons
                contentDescription = null,
                tint = if (theme == currentTheme) DuoBlue else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text,
                fontWeight = FontWeight.Bold,
                color = if (theme == currentTheme) DuoBlue else MaterialTheme.colorScheme.onSurface
            )
        }
        if (theme == currentTheme) {
            Icon(Icons.Default.Check, contentDescription = null, tint = DuoBlue)
        }
    }
}