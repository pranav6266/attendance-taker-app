package com.pranav.attendencetaker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoGreen
import com.pranav.attendencetaker.ui.theme.DuoBlue
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val currentMonth = YearMonth.now()
    val daysInMonth = (1..currentMonth.lengthOfMonth()).toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Month Header
            Text(
                text = "${currentMonth.month.name} ${currentMonth.year}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DuoBlue,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Calendar Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Weekday Headers
                items(listOf("S", "M", "T", "W", "T", "F", "S")) { day ->
                    Text(day, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
                }

                // Days
                items(daysInMonth) { day ->
                    val dateId = LocalDate.of(currentMonth.year, currentMonth.month, day).toString()
                    val hasLog = logs.any { it.id == dateId }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (hasLog) DuoGreen else Color.LightGray.copy(alpha = 0.3f))
                            .clickable(enabled = hasLog) {
                                navController.navigate(Screen.DayDetail.createRoute(dateId))
                            }
                    ) {
                        Text(
                            text = day.toString(),
                            color = if (hasLog) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simple Stats Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Classes Held: ${logs.size}")
                    // You can add more math here later (e.g., Average Attendance)
                }
            }
        }
    }
}