package com.pranav.attendencetaker.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.components.DaySummaryCard
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStats: () -> Unit,
    navController: androidx.navigation.NavController,
    onNavigateToStudents: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            DuoTopBar(
                progress = uiState.progress,
                onHistoryClick = onNavigateToStats,
                streak = 0
            )
        },
        bottomBar = {
            BottomControlDock(
                onUndo = { viewModel.onUndo() },
                canUndo = uiState.progress > 0 && uiState.studentsQueue.isNotEmpty(),
                onManageStudents = onNavigateToStudents,
                onProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundGray)
        ) {
            DotPatternBackground()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(color = DuoGreen, strokeWidth = 6.dp)

                    uiState.studentsQueue.isNotEmpty() -> {
                        val topStudent = uiState.studentsQueue[0]
                        val nextStudent = uiState.studentsQueue.getOrNull(1)

                        if (nextStudent != null) {
                            key(nextStudent.id) {
                                StudentCard(
                                    student = nextStudent,
                                    modifier = Modifier
                                        .scale(0.92f)
                                        .offset(y = 25.dp)
                                        .alpha(0.8f),
                                    isTopCard = false
                                )
                            }
                        }

                        key(topStudent.id) {
                            SwipeableStudentCard(
                                student = topStudent,
                                onSwipe = { status -> viewModel.onSwipe(topStudent, status) }
                            )
                        }
                    }

                    !uiState.isAttendanceFinalized && uiState.dailyLog != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Class Dismissed!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                "Great job today, Sensei.",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            DaySummaryCard(
                                log = uiState.dailyLog!!,
                                onClick = {
                                    navController.navigate(Screen.DayDetail.createRoute(uiState.dailyLog!!.id))
                                }
                            )
                        }
                    }

                    uiState.isAttendanceFinalized -> FinishedView()
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun DuoTopBar(
    progress: Float,
    onHistoryClick: () -> Unit,
    streak: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DuoIconButton(
            icon = Icons.Rounded.DateRange,
            color = DuoBlue,
            onClick = onHistoryClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E5E5))
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "progress"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(DuoGreen)
                    .padding(top = 4.dp, start = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(android.R.drawable.ic_lock_idle_low_battery),
                contentDescription = "Streak",
                tint = if (streak > 0) DuoYellow else Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun BottomControlDock(
    onUndo: () -> Unit,
    canUndo: Boolean,
    onManageStudents: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DuoIconButton(
            icon = Icons.Default.Refresh,
            color = DuoYellow,
            enabled = canUndo,
            size = 50.dp,
            onClick = onUndo
        )

        DuoLabelButton(
            text = "STUDENTS",
            icon = Icons.AutoMirrored.Filled.List,
            color = DuoBlue,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            onClick = onManageStudents
        )

        DuoIconButton(
            icon = Icons.Default.Person,
            color = Color(0xFF9C27B0),
            size = 50.dp,
            onClick = onProfile
        )
    }
}

@Composable
fun DuoIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateFloatAsState(if (isPressed) 4f else 0f, label = "offset")

    Box(
        modifier = Modifier
            .width(size)
            .height(size + 4.dp)
            .graphicsLayer { translationY = offsetY.dp.toPx() }
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = 4.dp)
                .background(color.darken(), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape)
                .border(2.dp, Color.Black.copy(alpha=0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun DuoLabelButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateFloatAsState(if (isPressed) 4f else 0f, label = "offset")

    Box(
        modifier = modifier
            .height(54.dp) // Adjusted to fit shadow
            .graphicsLayer { translationY = offsetY.dp.toPx() }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = 4.dp)
                .background(color.darken(), RoundedCornerShape(16.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(color, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
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

    val rotation = (offsetX.value / 40).coerceIn(-20f, 20f)

    val greenAlpha = (offsetX.value / 300f).coerceIn(0f, 1f)
    val redAlpha = (-offsetX.value / 300f).coerceIn(0f, 1f)
    val yellowAlpha = (offsetY.value / 300f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer(rotationZ = rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val swipeThreshold = 250f
                            when {
                                offsetX.value > swipeThreshold -> {
                                    offsetX.animateTo(screenWidth * 1.5f, tween(300))
                                    onSwipe(AttendanceStatus.PRESENT)
                                }
                                offsetX.value < -swipeThreshold -> {
                                    offsetX.animateTo(-screenWidth * 1.5f, tween(300))
                                    onSwipe(AttendanceStatus.ABSENT)
                                }
                                offsetY.value > swipeThreshold -> {
                                    offsetY.animateTo(screenHeight, tween(300))
                                    onSwipe(AttendanceStatus.LATE)
                                }
                                else -> {
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

        if (greenAlpha > 0) CardOverlay(DuoGreen, greenAlpha, "PRESENT")
        if (redAlpha > 0) CardOverlay(DuoRed, redAlpha, "ABSENT")
        if (yellowAlpha > 0) CardOverlay(DuoYellow, yellowAlpha, "LATE")
    }
}

@Composable
fun CardOverlay(color: Color, alpha: Float, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = alpha * 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
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
        border = BorderStroke(2.dp, Color(0xFFE5E5E5)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 10.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
            .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(getBeltColor(student.belt))
                    .border(6.dp, Color.White, CircleShape)
                    .border(4.dp, Color.Black.copy(alpha=0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.first().toString(),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = student.name,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4B4B4B)
            )

            Surface(
                color = BackgroundGray,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top=8.dp)
            ) {
                Text(
                    text = "${student.belt} Belt",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (student.currentStreak > 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(android.R.drawable.ic_lock_idle_low_battery), contentDescription = null, tint = DuoYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${student.currentStreak} day streak!", color = DuoYellowDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isTopCard) {
                Text("SWIPE TO MARK", fontSize = 14.sp, fontWeight=FontWeight.Bold, color = Color.LightGray, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- MISSING FUNCTIONS RESTORED BELOW ---

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

@Composable
fun DotPatternBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotRadius = 2.dp.toPx()
        val spacing = 40.dp.toPx()
        val rows = (size.height / spacing).toInt()
        val cols = (size.width / spacing).toInt()

        for (i in 0..rows) {
            for (j in 0..cols) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = dotRadius,
                    center = Offset(j * spacing + (if (i % 2 == 0) 0f else spacing / 2), i * spacing)
                )
            }
        }
    }
}

fun Color.darken(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}