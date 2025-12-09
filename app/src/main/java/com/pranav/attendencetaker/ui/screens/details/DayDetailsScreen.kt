package com.pranav.attendencetaker.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavController
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.theme.DuoGreen
import com.pranav.attendencetaker.ui.theme.DuoRed
import com.pranav.attendencetaker.ui.theme.DuoYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    dateId: String,
    navController: NavController
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()

    var log by remember { mutableStateOf<DailyLog?>(null) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }

    // Load Data
    LaunchedEffect(dateId) {
        log = repo.getLogByDate(dateId)
        students = repo.getActiveStudents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance: $dateId") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (log == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                // Focus of the Day Header
                item {
                    Text("Focus: ${log?.focus}", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(students) { student ->
                    val status = log!!.attendance[student.id] ?: AttendanceStatus.ABSENT

                    AttendanceRow(
                        student = student,
                        status = status,
                        onStatusChange = { newStatus ->
                            // Optimistic UI Update
                            val newMap = log!!.attendance.toMutableMap()
                            newMap[student.id] = newStatus
                            log = log!!.copy(attendance = newMap)

                            // Save to Firebase
                            scope.launch {
                                repo.updateStudentStatus(dateId, student.id, newStatus)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AttendanceRow(student: Student, status: AttendanceStatus, onStatusChange: (AttendanceStatus) -> Unit) {
    val color = when(status) {
        AttendanceStatus.PRESENT -> DuoGreen
        AttendanceStatus.ABSENT -> DuoRed
        AttendanceStatus.LATE -> DuoYellow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable {
                // Cycle through statuses on click: Present -> Absent -> Late -> Present
                val nextStatus = when(status) {
                    AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                    AttendanceStatus.ABSENT -> AttendanceStatus.LATE
                    AttendanceStatus.LATE -> AttendanceStatus.PRESENT
                }
                onStatusChange(nextStatus)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(student.belt, fontSize = 12.sp, color = Color.Gray)
        }

        // Status Badge
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = status.name,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}