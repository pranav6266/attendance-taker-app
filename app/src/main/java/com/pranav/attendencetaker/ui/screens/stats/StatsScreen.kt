package com.pranav.attendencetaker.ui.screens.stats

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pranav.attendencetaker.ui.components.*
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import java.time.LocalDate
import java.time.YearMonth

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
        DatePickerDialog(context, { _, year, month, _ -> viewModel.jumpToMonth(YearMonth.of(year, month + 1)) }, currentMonth.year, currentMonth.monthValue - 1, 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // FIXED: Button is now White
                DuoIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, color = Color.White, onClick = { navController.popBackStack() })
                Spacer(modifier = Modifier.width(16.dp))
                Text("HISTORY", fontSize = 20.sp, fontWeight = FontWeight.Black, color = getSubTextColor(), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("HELD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(logs.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = DuoBlue)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            DotPatternBackground()
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                // --- MONTH CONTROL ---
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    DuoIconButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, color = DuoBlue, onClick = { viewModel.previousMonth() })
                    Box(
                        modifier = Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(getCardFaceColor())
                            .border(2.dp, getShadowColor(), RoundedCornerShape(12.dp))
                            .clickable { datePickerDialog.show() }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = DuoBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${currentMonth.month.name} ${currentMonth.year}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DuoBlue)
                        }
                    }
                    DuoIconButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, color = DuoBlue, onClick = { viewModel.nextMonth() })
                }

                // --- CALENDAR GRID ---
                LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(360.dp)) {
                    items(listOf("S", "M", "T", "W", "T", "F", "S")) { day -> Text(day, textAlign = TextAlign.Center, color = getSubTextColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    items(firstDayOfWeek) { Spacer(modifier = Modifier) }
                    items(daysInMonth) { day ->
                        val date = LocalDate.of(currentMonth.year, currentMonth.month, day)
                        val dateId = date.toString()
                        val log = logs.find { it.id == dateId }
                        val hasLog = log != null
                        val isSelected = selectedDateId == dateId
                        CalendarDayItem(day = day, hasLog = hasLog, isSelected = isSelected, onClick = { if (hasLog) selectedDateId = if (isSelected) null else dateId })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // --- SUMMARY CARD ---
                if (selectedDateId != null) {
                    val selectedLog = logs.find { it.id == selectedDateId }
                    if (selectedLog != null) {
                        DaySummaryCard(log = selectedLog, onClick = { navController.navigate(Screen.DayDetail.createRoute(selectedDateId!!)) })
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Select a green day to view stats", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayItem(day: Int, hasLog: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, label = "scale")
    Box(modifier = Modifier.aspectRatio(1f).scale(scale).clip(CircleShape).clickable(enabled = hasLog, onClick = onClick), contentAlignment = Alignment.Center) {
        if (isSelected) Box(modifier = Modifier.fillMaxSize().border(3.dp, DuoBlue, CircleShape))
        Box(
            modifier = Modifier.fillMaxSize(if (isSelected) 0.8f else 1f).background(when { hasLog -> DuoGreen; else -> Color.Transparent }, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = day.toString(), color = if (hasLog) Color.White else getSubTextColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        if (!hasLog) Box(modifier = Modifier.size(4.dp).offset(y = 12.dp).background(getShadowColor(), CircleShape))
    }
}