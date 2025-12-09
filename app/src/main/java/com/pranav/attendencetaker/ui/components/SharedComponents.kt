package com.pranav.attendencetaker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import com.pranav.attendencetaker.ui.theme.DuoRed
import com.pranav.attendencetaker.ui.theme.DuoYellow

// --- COLOR HELPERS ---
@Composable
fun getCardFaceColor(): Color {
    // Soft Dark Gray for Dark Mode, White for Light Mode
    return if (isSystemInDarkTheme()) Color(0xFF252525) else Color.White
}

@Composable
fun getShadowColor(): Color {
    // Darker Black for Dark Mode shadow, Light Gray for Light Mode
    return if (isSystemInDarkTheme()) Color(0xFF111111) else Color(0xFFE5E5E5)
}

@Composable
fun getTextColor(): Color {
    return if (isSystemInDarkTheme()) Color(0xFFEEEEEE) else Color(0xFF4B4B4B)
}

@Composable
fun getSubTextColor(): Color {
    return if (isSystemInDarkTheme()) Color(0xFFAAAAAA) else Color.Gray
}

// --- ANIMATED STREAK FIRE ---
@Composable
fun AnimatedStreakFire(streak: Int, size: Dp = 32.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Streak",
            tint = if(streak > 0) DuoYellow else Color.Gray,
            modifier = Modifier.size(size).scale(if (streak > 0) scale else 1f).alpha(if (streak > 0) alpha else 0.5f)
        )
        if (streak > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = streak.toString(), fontWeight = FontWeight.Black, color = DuoYellow, fontSize = (size.value * 0.6).sp)
        }
    }
}

// --- 3D BUTTONS ---
@Composable
fun DuoIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateFloatAsState(if (isPressed) 4f else 0f, label = "offset")

    Box(
        modifier = Modifier
            .width(size).height(size + 4.dp)
            .graphicsLayer { translationY = offsetY.dp.toPx() }
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.size(size).offset(y = 4.dp).background(color.darken(), CircleShape))
        Box(
            modifier = Modifier.size(size).background(color, CircleShape).border(2.dp, Color.Black.copy(alpha=0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Icon tint is Dark Gray if button is White/Light, otherwise White
            val iconTint = if (color == Color.White || color == Color.LightGray) Color(0xFF4B4B4B) else Color.White
            Icon(icon, contentDescription = null, tint = iconTint)
        }
    }
}

@Composable
fun DuoLabelButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateFloatAsState(if (isPressed) 4f else 0f, label = "offset")

    Box(
        modifier = modifier.height(54.dp).graphicsLayer { translationY = offsetY.dp.toPx() }.alpha(if (enabled) 1f else 0.5f)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).offset(y = 4.dp).background(color.darken(), RoundedCornerShape(16.dp)))
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp).background(color, RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black.copy(alpha=0.05f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text.uppercase(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, letterSpacing = 1.sp)
        }
    }
}

// --- SUMMARY CARD (UPDATED) ---
@Composable
fun DaySummaryCard(
    log: DailyLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentCount = log.attendance.values.count { it == AttendanceStatus.PRESENT }
    val absentCount = log.attendance.values.count { it == AttendanceStatus.ABSENT }
//    val lateCount = log.attendance.values.count { it == AttendanceStatus.LATE }

    val faceColor = getCardFaceColor()
    val shadowColor = getShadowColor()
    val textColor = getSubTextColor()

    Box(modifier = modifier.fillMaxWidth().clickable { onClick() }) {
        // Shadow
        Box(modifier = Modifier.fillMaxWidth().height(184.dp).offset(y = 4.dp).background(shadowColor, RoundedCornerShape(24.dp)))

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(faceColor, RoundedCornerShape(24.dp)) // Dynamic Color
                .border(2.dp, shadowColor, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SUMMARY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor, letterSpacing = 1.sp)
                if (log.finalized) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LOCKED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DuoGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Finalized", tint = DuoGreen, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text("UNFINALIZED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DuoYellow)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBadge(count = presentCount, label = "Present", color = DuoGreen)
//                StatBadge(count = lateCount, label = "Late", color = DuoYellow)
                StatBadge(count = absentCount, label = "Absent", color = DuoRed)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = if (log.finalized) "VIEW DETAILS" else "FINALIZE ATTENDANCE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DuoBlue)
            }
        }
    }
}

@Composable
fun StatBadge(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}

// --- UTILS ---
@Composable
fun DotPatternBackground() {
    val dotColor = if(isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotRadius = 2.dp.toPx()
        val spacing = 40.dp.toPx()
        val rows = (size.height / spacing).toInt()
        val cols = (size.width / spacing).toInt()
        for (i in 0..rows) {
            for (j in 0..cols) {
                drawCircle(color = dotColor, radius = dotRadius, center = Offset(j * spacing + (if (i % 2 == 0) 0f else spacing / 2), i * spacing))
            }
        }
    }
}

fun Color.darken(factor: Float = 0.8f): Color {
    return Color(red = this.red * factor, green = this.green * factor, blue = this.blue * factor, alpha = this.alpha)
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