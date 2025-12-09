package com.pranav.attendencetaker.ui.screens.stats

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pranav.attendencetaker.ui.components.DaySummaryCard
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedDateId by remember { mutableStateOf<String?>(null) }

    val daysInMonth = (1..currentMonth.lengthOfMonth()).toList()
    val firstDayOfWeek = LocalDate.of(currentMonth.year, currentMonth.month, 1).dayOfWeek.value % 7

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, _ ->
                viewModel.jumpToMonth(YearMonth.of(year, month + 1))
            },
            currentMonth.year,
            currentMonth.monthValue - 1,
            1
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Classes Held", fontSize = 10.sp, color = Color.Gray, lineHeight = 10.sp)
                        Text(
                            text = logs.size.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DuoBlue,
                            lineHeight = 20.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${currentMonth.month.name} ${currentMonth.year}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Pick Date", modifier = Modifier.size(16.dp), tint = DuoBlue)
                }

                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(350.dp)
            ) {
                items(listOf("S", "M", "T", "W", "T", "F", "S")) { day ->
                    Text(day, textAlign = TextAlign.Center, color = Color.Gray)
                }

                items(firstDayOfWeek) { Spacer(modifier = Modifier) }

                items(daysInMonth) { day ->
                    val date = LocalDate.of(currentMonth.year, currentMonth.month, day)
                    val dateId = date.toString()
                    val log = logs.find { it.id == dateId }
                    val hasLog = log != null
                    val isSelected = selectedDateId == dateId

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> DuoBlue
                                    hasLog -> DuoGreen
                                    else -> Color.LightGray.copy(alpha = 0.3f)
                                }
                            )
                            .clickable(enabled = hasLog) {
                                selectedDateId = if (selectedDateId == dateId) null else dateId
                            }
                            .then(if (isSelected) Modifier.border(2.dp, Color.Black, CircleShape) else Modifier)
                    ) {
                        Text(
                            text = day.toString(),
                            color = if (hasLog || isSelected) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedDateId != null) {
                val selectedLog = logs.find { it.id == selectedDateId }
                if (selectedLog != null) {
                    DaySummaryCard(
                        log = selectedLog,
                        onClick = {
                            navController.navigate(Screen.DayDetail.createRoute(selectedDateId!!))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Select a highlighted date to view summary", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}