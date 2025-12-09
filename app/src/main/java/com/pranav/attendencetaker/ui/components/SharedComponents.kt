package com.pranav.attendencetaker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import com.pranav.attendencetaker.ui.theme.DuoRed
import com.pranav.attendencetaker.ui.theme.DuoYellow

@Composable
fun DaySummaryCard(
    log: DailyLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentCount = log.attendance.values.count { it == AttendanceStatus.PRESENT }
    val absentCount = log.attendance.values.count { it == AttendanceStatus.ABSENT }
    val lateCount = log.attendance.values.count { it == AttendanceStatus.LATE }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Summary: ${log.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DuoBlue)

                if (log.finalized) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Finalized", tint = DuoGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBadge(count = presentCount, label = "Present", color = DuoGreen)
                StatBadge(count = lateCount, label = "Late", color = DuoYellow)
                StatBadge(count = absentCount, label = "Absent", color = DuoRed)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val footerText = if (log.finalized) "Tap to view details" else "Tap to view details & finalize"

            Text(
                text = footerText,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun StatBadge(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}