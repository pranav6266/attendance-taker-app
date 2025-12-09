package com.pranav.attendencetaker.ui.screens.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.theme.DuoBlue
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
    val context = LocalContext.current
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()

    var log by remember { mutableStateOf<DailyLog?>(null) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }

    // Focus Editing State
    var focusText by remember { mutableStateOf("") }
    var isFocusDirty by remember { mutableStateOf(false) }

    var showFinalizeDialog by remember { mutableStateOf(false) }

    // Load Data
    LaunchedEffect(dateId) {
        val fetchedLog = repo.getLogByDate(dateId)
        log = fetchedLog
        focusText = fetchedLog?.focus ?: ""
        students = repo.getActiveStudents()
    }

    fun onConfirmFinalize() {
        scope.launch {
            try {
                repo.finalizeLog(dateId)
                log = log?.copy(finalized = true)
                showFinalizeDialog = false
                Toast.makeText(context, "Attendance Finalized!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Error finalizing: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun saveFocus() {
        scope.launch {
            try {
                repo.updateFocus(dateId, focusText)
                log = log?.copy(focus = focusText)
                isFocusDirty = false
                Toast.makeText(context, "Focus updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving focus", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showFinalizeDialog) {
        val lateCount = log?.attendance?.values?.count { it == AttendanceStatus.LATE } ?: 0
        AlertDialog(
            onDismissRequest = { showFinalizeDialog = false },
            title = { Text("Finalize Attendance?") },
            text = {
                Column {
                    Text("This will lock the attendance record for today.")
                    if (lateCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Warning: You have $lateCount student(s) marked as LATE. If you finalize now without changing them, they will be recorded as Late.",
                            color = DuoRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onConfirmFinalize() }, colors = ButtonDefaults.buttonColors(containerColor = DuoBlue)) {
                    Text("Yes, Finalize")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance: $dateId") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (log?.finalized == true) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            )
        },
        bottomBar = {
            if (log != null && !log!!.finalized) {
                Button(
                    onClick = { showFinalizeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoBlue)
                ) {
                    Text("Finalize Attendance", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        if (log == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {

                // --- FOCUS EDIT SECTION ---
                item {
                    Text("Class Focus", fontSize = 14.sp, color = DuoBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = focusText,
                            onValueChange = {
                                focusText = it
                                isFocusDirty = true
                            },
                            enabled = !log!!.finalized, // Disable if finalized
                            label = { Text("What did we learn today?") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )

                        // Show Save button only if text changed and not finalized
                        if (isFocusDirty && !log!!.finalized) {
                            IconButton(onClick = { saveFocus() }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = DuoGreen)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- STUDENTS LIST ---
                items(students) { student ->
                    val status = log!!.attendance[student.id] ?: AttendanceStatus.ABSENT

                    AttendanceRow(
                        student = student,
                        status = status,
                        isReadOnly = log!!.finalized,
                        onStatusChange = { newStatus ->
                            val newMap = log!!.attendance.toMutableMap()
                            newMap[student.id] = newStatus
                            log = log!!.copy(attendance = newMap)

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
fun AttendanceRow(
    student: Student,
    status: AttendanceStatus,
    isReadOnly: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    val color = when(status) {
        AttendanceStatus.PRESENT -> DuoGreen
        AttendanceStatus.ABSENT -> DuoRed
        AttendanceStatus.LATE -> DuoYellow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isReadOnly) Color.White.copy(alpha = 0.6f) else Color.White)
            .clickable(enabled = !isReadOnly) {
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