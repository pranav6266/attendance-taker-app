package com.pranav.attendencetaker.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange // <--- The Calendar Icon
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStats: () -> Unit // <--- New Callback for Navigation
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            AttendanceTopBar(
                progress = uiState.progress,
                onUndo = { viewModel.onUndo() },
                canUndo = uiState.progress > 0 && !uiState.finished,
                onHistoryClick = onNavigateToStats // <--- Pass it to the bar
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(color = DuoGreen)
                uiState.finished -> FinishedView()
                uiState.studentsQueue.isNotEmpty() -> {
                    val topStudent = uiState.studentsQueue[0]
                    val nextStudent = uiState.studentsQueue.getOrNull(1)

                    // Background Card (Static)
                    if (nextStudent != null) {
                        key(nextStudent.id) {
                            StudentCard(
                                student = nextStudent,
                                modifier = Modifier.scale(0.9f).alpha(0.7f),
                                isTopCard = false
                            )
                        }
                    }

                    // Top Card (Swipeable) - Wrapped in key() to fix "stuck" animation
                    key(topStudent.id) {
                        SwipeableStudentCard(
                            student = topStudent,
                            onSwipe = { status -> viewModel.onSwipe(topStudent, status) }
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun AttendanceTopBar(
    progress: Float,
    onUndo: () -> Unit,
    canUndo: Boolean,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo Button
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.alpha(if (canUndo) 1f else 0.3f)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Undo", tint = DuoBlue)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(50)),
            color = DuoGreen,
            trackColor = Color.LightGray.copy(alpha = 0.3f),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // History / Calendar Button (New!)
        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier
                .background(Color.White, CircleShape)
                .border(2.dp, BackgroundGray, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = "History",
                tint = DuoBlue
            )
        }
    }
}

@Composable
fun SwipeableStudentCard(
    student: Student,
    onSwipe: (AttendanceStatus) -> Unit
) {
    val context = LocalContext.current
    val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val rotation = (offsetX.value / 60).coerceIn(-40f, 40f)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer(rotationZ = rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val swipeThreshold = 300f
                            when {
                                offsetX.value > swipeThreshold -> {
                                    // Swipe Right (Present)
                                    offsetX.animateTo(screenWidth * 1.5f, tween(300))
                                    onSwipe(AttendanceStatus.PRESENT)
                                }
                                offsetX.value < -swipeThreshold -> {
                                    // Swipe Left (Absent)
                                    offsetX.animateTo(-screenWidth * 1.5f, tween(300))
                                    onSwipe(AttendanceStatus.ABSENT)
                                }
                                offsetY.value > swipeThreshold -> {
                                    // Swipe Down (Late)
                                    offsetY.animateTo(screenHeight, tween(300))
                                    onSwipe(AttendanceStatus.LATE)
                                }
                                else -> {
                                    // Snap back
                                    launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                    launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        StudentCard(student = student, isTopCard = true)

        // Visual Labels
        if (offsetX.value > 100) {
            Text("PRESENT", color = DuoGreen, modifier = Modifier.align(Alignment.CenterStart).rotate(-20f).padding(20.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium)
        } else if (offsetX.value < -100) {
            Text("ABSENT", color = DuoRed, modifier = Modifier.align(Alignment.CenterEnd).rotate(20f).padding(20.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium)
        } else if (offsetY.value > 100) {
            Text("LATE", color = DuoYellow, modifier = Modifier.align(Alignment.TopCenter).padding(20.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium)
        }
    }
}

@Composable
fun StudentCard(
    student: Student,
    modifier: Modifier = Modifier,
    isTopCard: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 8.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(getBeltColor(student.belt))
                    .border(4.dp, Color.Black.copy(alpha=0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.first().toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = student.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(text = "${student.belt} Belt", fontSize = 18.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            if (student.currentStreak > 2) {
                Surface(
                    color = DuoYellow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, DuoYellow, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using a standard lock icon as placeholder for fire
                        Icon(painter = painterResource(android.R.drawable.ic_lock_idle_low_battery), contentDescription = null, tint = DuoYellow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${student.currentStreak} Day Streak!", color = DuoYellowDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isTopCard) {
                Text("Swipe Right for Present", fontSize = 12.sp, color = Color.LightGray)
                Text("Swipe Left for Absent", fontSize = 12.sp, color = Color.LightGray)
                Text("Swipe Down for Late", fontSize = 12.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun FinishedView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("All Done!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DuoGreen)
        Text("Attendance finalized for today.", fontSize = 16.sp, color = Color.Gray)
    }
}

fun getBeltColor(belt: String): Color {
    return when(belt.lowercase()) {
        "white" -> Color.LightGray
        "yellow" -> Color(0xFFFFEB3B)
        "green" -> Color(0xFF4CAF50)
        "blue" -> Color(0xFF2196F3)
        "red" -> Color(0xFFF44336)
        "black" -> Color.Black
        else -> Color.Gray
    }
}

fun Modifier.rotate(degrees: Float) = this.graphicsLayer(rotationZ = degrees)