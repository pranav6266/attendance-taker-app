package com.pranav.attendencetaker.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.components.AnimatedStreakFire
import com.pranav.attendencetaker.ui.components.DotPatternBackground
import com.pranav.attendencetaker.ui.components.DuoIconButton
import com.pranav.attendencetaker.ui.components.getBeltColor
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoBlue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- VIEWMODEL ---
// This class was likely missing or unresolved in your previous file
class StudentListViewModel : ViewModel() {
    private val repo = FirestoreRepository()
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students = _students.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getActiveStudentsFlow().collect { updatedList ->
                _students.value = updatedList
            }
        }
    }
}

// --- SCREEN ---
@Composable
fun StudentListScreen(
    navController: NavController,
    viewModel: StudentListViewModel = viewModel()
) {
    val students by viewModel.students.collectAsState()

    // 1. Set Scaffold Background for Dark Mode
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2. Fix Back Button Visibility
                DuoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "STUDENTS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))

                // Add Button
                DuoIconButton(
                    icon = Icons.Default.Add,
                    color = DuoBlue,
                    onClick = { navController.navigate(Screen.StudentEntry.createRoute()) }
                )
            }
        }
    ) { padding ->
        // 3. Set Root Box Background for Dark Mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DotPatternBackground()

            if (students.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(students) { student ->
                        StudentListItem(
                            student = student,
                            onClick = {
                                // 'id' error happens if imports are wrong or Student class isn't seen
                                navController.navigate(Screen.StudentEntry.createRoute(student.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentListItem(student: Student, onClick: () -> Unit) {
    // Dynamic colors
    val shadowColor = if(androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2C2C2C) else Color(0xFFE5E5E5)
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable(onClick = onClick)
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 4.dp)
                .background(shadowColor, RoundedCornerShape(16.dp))
        )
        // Card Face
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(cardColor, RoundedCornerShape(16.dp))
                .border(2.dp, shadowColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Belt/Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getBeltColor(student.belt)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    student.name.take(1),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Text(
                    "${student.belt} Belt",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            // Streak Fire Animation
            AnimatedStreakFire(streak = student.currentStreak)
        }
    }
}