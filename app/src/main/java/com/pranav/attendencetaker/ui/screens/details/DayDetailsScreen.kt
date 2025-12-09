package com.pranav.attendencetaker.ui.screens.details

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.components.*
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import com.pranav.attendencetaker.ui.theme.DuoRed
import com.pranav.attendencetaker.ui.theme.DuoYellow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun DayDetailScreen(
    dateId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()

    var log by remember { mutableStateOf<DailyLog?>(null) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var focusText by remember { mutableStateOf("") }
    var isFocusDirty by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dateId) {
        val fetchedLog = repo.getLogByDate(dateId)
        log = fetchedLog
        focusText = fetchedLog?.focus ?: ""
        val activeStudents = repo.getActiveStudents()
        val logStudentIds = fetchedLog?.attendance?.keys ?: emptySet()
        val activeIds = activeStudents.map { it.id }.toSet()
        val missingIds = logStudentIds - activeIds
        val archivedStudents = missingIds.map { id -> async { repo.getStudentById(id) } }.awaitAll().filterNotNull()
        students = (activeStudents + archivedStudents).sortedBy { it.name }
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
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun saveFocus() {
        focusManager.clearFocus()
        scope.launch {
            try {
                repo.updateFocus(dateId, focusText)
                log = log?.copy(focus = focusText)
                isFocusDirty = false
                Toast.makeText(context, "Focus updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { }
        }
    }

    if (showFinalizeDialog) {
        val lateCount = log?.attendance?.values?.count { it == AttendanceStatus.LATE } ?: 0
        AlertDialog(
            onDismissRequest = { showFinalizeDialog = false },
            title = { Text("Finalize Class?", fontWeight = FontWeight.Bold, color = getTextColor()) },
            text = {
                Column {
                    Text("This will lock the attendance record.", color = getSubTextColor())
                    if (lateCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Warning: $lateCount student(s) marked LATE.", color = DuoRed, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onConfirmFinalize() }, colors = ButtonDefaults.buttonColors(containerColor = DuoBlue)) { Text("FINALIZE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeDialog = false }) { Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold) }
            },
            containerColor = getCardFaceColor()
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // FIXED: Button is now White for visibility
                DuoIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, color = Color.White, onClick = { navController.popBackStack() })
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("ATTENDANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = getSubTextColor())
                    Text(dateId, fontSize = 20.sp, fontWeight = FontWeight.Black, color = getTextColor())
                }
                Spacer(modifier = Modifier.weight(1f))
                if (log?.finalized == true) Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.LightGray)
            }
        },
        bottomBar = {
            if (log != null && !log!!.finalized) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    DuoLabelButton(text = "FINALIZE ATTENDANCE", color = DuoBlue, modifier = Modifier.fillMaxWidth(), onClick = { showFinalizeDialog = true })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }) {
            DotPatternBackground()

            if (log == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DuoBlue) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Column {
                            Text("CLASS FOCUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = getSubTextColor(), letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            // FIXED: Focus box now uses Dynamic Colors
                            val shadowColor = getShadowColor()
                            val faceColor = getCardFaceColor()
                            val borderColor = if (isFocusDirty) DuoBlue else shadowColor

                            Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                                Box(modifier = Modifier.fillMaxSize().offset(y = 4.dp).background(shadowColor, RoundedCornerShape(16.dp)))
                                Row(
                                    modifier = Modifier.fillMaxSize().background(faceColor, RoundedCornerShape(16.dp))
                                        .border(2.dp, borderColor, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = focusText,
                                        onValueChange = { focusText = it; isFocusDirty = true },
                                        enabled = !log!!.finalized,
                                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = getTextColor()),
                                        cursorBrush = SolidColor(DuoBlue),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isFocusDirty && !log!!.finalized) {
                                        Text("SAVE", color = DuoBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { saveFocus() })
                                    }
                                }
                            }
                        }
                    }

                    items(students) { student ->
                        val status = log!!.attendance[student.id] ?: AttendanceStatus.ABSENT
                        DuoAttendanceRow(
                            student = student,
                            status = status,
                            isReadOnly = log!!.finalized,
                            onStatusChange = { newStatus ->
                                val newMap = log!!.attendance.toMutableMap(); newMap[student.id] = newStatus; log = log!!.copy(attendance = newMap)
                                scope.launch { repo.updateStudentStatus(dateId, student.id, newStatus) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuoAttendanceRow(
    student: Student,
    status: AttendanceStatus,
    isReadOnly: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    val statusColor by animateColorAsState(targetValue = when(status) {
        AttendanceStatus.PRESENT -> DuoGreen
        AttendanceStatus.ABSENT -> DuoRed
        AttendanceStatus.LATE -> DuoYellow
    }, label = "color")

    // FIXED: Colors for Dark Mode
    val shadowColor = getShadowColor()
    val cardColor = getCardFaceColor()
    val textColor = getTextColor()

    Box(modifier = Modifier.fillMaxWidth().height(84.dp)) {
        Box(modifier = Modifier.fillMaxSize().offset(y = 4.dp).background(shadowColor, RoundedCornerShape(16.dp)))
        Row(
            modifier = Modifier.fillMaxSize().background(cardColor, RoundedCornerShape(16.dp))
                .border(2.dp, shadowColor, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if(isSystemInDarkTheme()) Color(0xFF333333) else Color(0xFFF7F7F7), CircleShape), contentAlignment = Alignment.Center) {
                Text(student.name.take(1), fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                    if (!student.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("(Archived)", fontSize = 10.sp, color = DuoRed, fontWeight = FontWeight.Bold)
                    }
                }
                Text(student.belt, fontSize = 12.sp, color = getSubTextColor(), fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(statusColor.copy(alpha=0.15f))
                    .border(2.dp, statusColor, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isReadOnly) {
                        val nextStatus = when(status) { AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT; AttendanceStatus.ABSENT -> AttendanceStatus.LATE; AttendanceStatus.LATE -> AttendanceStatus.PRESENT }
                        onStatusChange(nextStatus)
                    }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = status.name, color = statusColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}