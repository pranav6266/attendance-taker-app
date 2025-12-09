package com.pranav.attendencetaker.ui.screens.entry

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.ui.components.*
import com.pranav.attendencetaker.ui.theme.DuoBlue
import com.pranav.attendencetaker.ui.theme.DuoGreen
import com.pranav.attendencetaker.ui.theme.DuoRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- VIEWMODEL ---
class StudentEntryViewModel : ViewModel() {
    private val repo = FirestoreRepository()

    private val _student = MutableStateFlow<Student?>(null)
    val student = _student.asStateFlow()

    // Form State
    var name by mutableStateOf("")
    var age by mutableStateOf("")
    var phone by mutableStateOf("")
    var belt by mutableStateOf("White")

    fun loadStudent(id: String) {
        viewModelScope.launch {
            val s = repo.getStudentById(id)
            if (s != null) {
                _student.value = s
                name = s.name
                age = s.age.toString()
                phone = s.phoneNumber
                belt = s.belt
            }
        }
    }

    fun saveStudent(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val newStudent = _student.value?.copy(
                name = name,
                age = age.toIntOrNull() ?: 0,
                phoneNumber = phone,
                belt = belt
            ) ?: Student(
                name = name,
                age = age.toIntOrNull() ?: 0,
                phoneNumber = phone,
                belt = belt
            )

            repo.saveStudent(newStudent)
            onSuccess()
        }
    }

    fun deleteStudent(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.deleteStudent(id)
            onSuccess()
        }
    }
}

// --- SCREEN ---
@Composable
fun StudentEntryScreen(
    studentId: String?,
    navController: NavController,
    viewModel: StudentEntryViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = !studentId.isNullOrEmpty()

    LaunchedEffect(studentId) {
        if (studentId != null) {
            viewModel.loadStudent(studentId)
        }
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
                    if(isEditMode) "EDIT STUDENT" else "NEW STUDENT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))

                if (isEditMode) {
                    DuoIconButton(
                        icon = Icons.Default.Delete,
                        color = DuoRed,
                        onClick = {
                            viewModel.deleteStudent(studentId!!) {
                                Toast.makeText(context, "Student deleted", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                DuoLabelButton(
                    text = "SAVE PROFILE",
                    color = DuoGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (viewModel.name.isBlank()) {
                            Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveStudent {
                                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Name Input
            DuoTextField(
                label = "FULL NAME",
                value = viewModel.name,
                onValueChange = { viewModel.name = it }
            )

            // Age Input
            DuoTextField(
                label = "AGE",
                value = viewModel.age,
                onValueChange = { viewModel.age = it },
                keyboardType = KeyboardType.Number
            )

            // Phone Input
            DuoTextField(
                label = "PHONE NUMBER",
                value = viewModel.phone,
                onValueChange = { viewModel.phone = it },
                keyboardType = KeyboardType.Phone
            )

            // Belt Selector
            Column {
                Text(
                    "BELT COLOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val belts = listOf("White", "Yellow", "Green", "Blue", "Red", "Black")
                    items(belts) { belt ->
                        BeltChip(
                            colorName = belt,
                            isSelected = viewModel.belt == belt,
                            onClick = { viewModel.belt = belt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DuoBlue,
                unfocusedBorderColor = Color(0xFFE5E5E5),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF7F7F7)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
        )
    }
}

@Composable
fun BeltChip(
    colorName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = getBeltColor(colorName)
    val borderColor = if (isSelected) DuoBlue else Color(0xFFE5E5E5)
    val borderWidth = if (isSelected) 4.dp else 2.dp

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(color)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
        }
    }
}