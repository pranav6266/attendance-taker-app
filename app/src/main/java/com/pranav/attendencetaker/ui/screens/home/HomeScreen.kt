package com.pranav.attendencetaker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.pranav.attendencetaker.ui.components.DotPatternBackground
import com.pranav.attendencetaker.ui.components.DuoIconButton
import com.pranav.attendencetaker.ui.components.DuoLabelButton
import com.pranav.attendencetaker.ui.components.getBeltColor
import com.pranav.attendencetaker.ui.components.isAppInDarkTheme
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeHeaderBar(
                progress = uiState.progress,
                streak = 0 // Connect to real data later if needed
            )
        },
        bottomBar = {
            BottomNavDock(
                onHistory = onNavigateToStats,
                onStudents = onNavigateToStudents,
                onProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DotPatternBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                GreetingHeader()

                // --- LEGEND ROW ---
                StatusLegend()

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(300.dp)
                            .offset(y = 50.dp)
                    ) {
                        drawCircle(color = Color.Black.copy(alpha = 0.03f))
                    }

                    when {
                        uiState.isLoading -> CircularProgressIndicator(
                            color = DuoGreen,
                            strokeWidth = 6.dp
                        )

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
                                            .alpha(0.6f),
                                        isTopCard = false
                                    )
                                }
                            }

                            key(topStudent.id) {
                                SwipeableStudentCard(
                                    student = topStudent,
                                    onSwipe = { status ->
                                        viewModel.onSwipe(topStudent, status)
                                    }
                                )
                            }
                        }

                        !uiState.isAttendanceFinalized && uiState.dailyLog != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Class Dismissed!",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoBlue
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
                                        navController.navigate(
                                            Screen.DayDetail.createRoute(
                                                uiState.dailyLog!!.id
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        uiState.isAttendanceFinalized -> FinishedView()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            val showUndo = uiState.progress > 0 && uiState.studentsQueue.isNotEmpty()
            AnimatedVisibility(
                visible = showUndo,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp)
            ) {
                DuoIconButton(
                    icon = Icons.Default.Refresh,
                    color = DuoYellow,
                    size = 48.dp,
                    onClick = { viewModel.onUndo() }
                )
            }
        }
    }
}

// --- STATUS LEGEND ---
@Composable
fun StatusLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = "Present", color = DuoGreen)
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(label = "Absent", color = DuoRed)
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(label = "Late", color = DuoYellow)
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HomeHeaderBar(progress: Float, streak: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_fire")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isAppInDarkTheme()) Color(0xFF333333) else Color(
                        0xFFE5E5E5
                    )
                )
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
                    .padding(top = 6.dp, start = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(android.R.drawable.ic_lock_idle_low_battery),
                contentDescription = "Streak",
                tint = if (streak >= 0) DuoYellow else Color.LightGray,
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale)
                    .alpha(alpha)
            )
            if (streak > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    streak.toString(),
                    fontWeight = FontWeight.Bold,
                    color = DuoYellow,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun GreetingHeader() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = date.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Text(
            text = "Time to Train!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun BottomNavDock(
    onHistory: () -> Unit,
    onStudents: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DuoIconButton(
            icon = Icons.Rounded.DateRange,
            color = DuoBlue,
            size = 50.dp,
            onClick = onHistory
        )
        DuoLabelButton(
            text = "STUDENTS",
            icon = Icons.AutoMirrored.Filled.List,
            color = DuoBlue,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            onClick = onStudents
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

    // Dominant Axis Logic
    val isHorizontal = abs(offsetX.value) > abs(offsetY.value)

    // Calculate Alphas based on Dominance
    val greenAlpha =
        if (isHorizontal && offsetX.value > 0) (offsetX.value / 300f).coerceIn(0f, 1f) else 0f
    val redAlpha =
        if (isHorizontal && offsetX.value < 0) (-offsetX.value / 300f).coerceIn(0f, 1f) else 0f
    val yellowAlpha =
        if (!isHorizontal && offsetY.value > 0) (offsetY.value / 300f).coerceIn(0f, 1f) else 0f

    // Choose a single overlay color & alpha (no color mixing, stays inside card)
    val overlayColor: Color? = when {
        greenAlpha > 0f -> DuoGreen
        redAlpha > 0f -> DuoRed
        yellowAlpha > 0f -> DuoYellow
        else -> null
    }
    val overlayAlpha: Float = maxOf(greenAlpha, redAlpha, yellowAlpha)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer(rotationZ = rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val swipeThreshold = 250f
                            val isHorizontalDrag =
                                abs(offsetX.value) > abs(offsetY.value)

                            when {
                                isHorizontalDrag && offsetX.value > swipeThreshold -> {
                                    offsetX.animateTo(screenWidth * 1.5f, tween(300))
                                    onSwipe(AttendanceStatus.PRESENT)
                                }

                                isHorizontalDrag && offsetX.value < -swipeThreshold -> {
                                    offsetX.animateTo(-screenWidth * 1.5f, tween(300))
                                    onSwipe(AttendanceStatus.ABSENT)
                                }

                                !isHorizontalDrag && offsetY.value > swipeThreshold -> {
                                    offsetY.animateTo(screenHeight, tween(300))
                                    onSwipe(AttendanceStatus.LATE)
                                }

                                else -> {
                                    launch {
                                        offsetX.animateTo(
                                            0f,
                                            spring(stiffness = Spring.StiffnessMedium)
                                        )
                                    }
                                    launch {
                                        offsetY.animateTo(
                                            0f,
                                            spring(stiffness = Spring.StiffnessMedium)
                                        )
                                    }
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
            .padding(8.dp)
    ) {
        StudentCard(
            student = student,
            isTopCard = true,
            modifier = Modifier.padding(0.dp),
            overlayColor = overlayColor,
            overlayAlpha = overlayAlpha
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    modifier: Modifier = Modifier,
    isTopCard: Boolean,
    overlayColor: Color? = null,
    overlayAlpha: Float = 0f
) {
    val cardShape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(8.dp),
        shape = cardShape,
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (isAppInDarkTheme()) Color(0xFF333333) else Color(0xFFE5E5E5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 10.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(getBeltColor(student.belt))
                        .border(6.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .border(4.dp, Color.Black.copy(alpha = 0.1f), CircleShape),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 40.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "${student.belt} Belt",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (student.currentStreak > 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_lock_idle_low_battery),
                            contentDescription = null,
                            tint = DuoYellow
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${student.currentStreak} day streak!",
                            color = DuoYellowDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isTopCard) {
                    Text(
                        "SWIPE TO MARK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Overlay strictly inside the Card (no bleeding)
            if (overlayColor != null && overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(cardShape)
                        .background(overlayColor.copy(alpha = overlayAlpha * 0.6f))
                )
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
