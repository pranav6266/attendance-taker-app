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
import androidx.compose.runtime.*
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
import com.pranav.attendencetaker.ui.components.*
import com.pranav.attendencetaker.ui.navigation.Screen
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- VIEWMODEL ---
class StudentListViewModel : ViewModel() {
    private val repo = FirestoreRepository()
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students = _students.asStateFlow()

    fun loadStudents() {
        viewModelScope.launch {
            _students.value = repo.getActiveStudents()
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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Replace LaunchedEffect(Unit) with this:
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadStudents() // Reloads every time you see the screen
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DuoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    color = Color.LightGray,
                    onClick = { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "STUDENTS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                .background(Color(0xFFE5E5E5), RoundedCornerShape(16.dp))
        )
        // Card Face
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFE5E5E5), RoundedCornerShape(16.dp))
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
                    color = Color(0xFF4B4B4B)
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