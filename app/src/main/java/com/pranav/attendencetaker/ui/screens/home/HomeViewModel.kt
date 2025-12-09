package com.pranav.attendencetaker.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack

class HomeViewModel : ViewModel() {

    private val repository = FirestoreRepository()

    // The UI observes this state
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Internal trackers
    private val _attendanceMap = mutableMapOf<String, AttendanceStatus>()
    private val _actionStack = Stack<Pair<Student, AttendanceStatus>>() // For Undo logic

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Fetch all students
            val allStudents = repository.getActiveStudents()

            // 2. Check if we already started attendance today (Resume logic)
            val todayLog = repository.getTodayLog()

            val remainingStudents = if (todayLog != null) {
                // Determine who is NOT in the log yet
                _attendanceMap.putAll(todayLog.attendance)
                allStudents.filter { student ->
                    !todayLog.attendance.containsKey(student.id)
                }
            } else {
                allStudents
            }

            // 3. Update UI
            _uiState.update {
                it.copy(
                    isLoading = false,
                    studentsQueue = remainingStudents,
                    totalCount = allStudents.size,
                    progress = calculateProgress(allStudents.size, remainingStudents.size)
                )
            }
        }
    }

    // --- USER ACTIONS ---

    fun onSwipe(student: Student, status: AttendanceStatus) {
        // 1. Add to our local map
        _attendanceMap[student.id] = status

        // 2. Add to Undo Stack
        _actionStack.push(Pair(student, status))

        // 3. Update the UI: Remove student from queue, update progress
        _uiState.update { currentState ->
            val newQueue = currentState.studentsQueue - student
            val isFinished = newQueue.isEmpty()

            // If finished, auto-save to backend
            if (isFinished) saveToBackend()

            currentState.copy(
                studentsQueue = newQueue,
                progress = calculateProgress(currentState.totalCount, newQueue.size),
                finished = isFinished
            )
        }
    }

    fun onUndo() {
        if (_actionStack.isEmpty()) return

        // 1. Pop the last action
        val lastAction = _actionStack.pop()
        val student = lastAction.first

        // 2. Remove from attendance map
        _attendanceMap.remove(student.id)

        // 3. Add student BACK to the TOP of the queue (index 0)
        _uiState.update { currentState ->
            val newQueue = listOf(student) + currentState.studentsQueue

            currentState.copy(
                studentsQueue = newQueue,
                progress = calculateProgress(currentState.totalCount, newQueue.size),
                finished = false // If we undid the last one, we aren't finished
            )
        }
    }

    // --- HELPER LOGIC ---

    private fun saveToBackend() {
        viewModelScope.launch {
            try {
                // Save whatever we have (Present, Absent, Late)
                repository.saveAttendanceBatch(
                    attendanceMap = _attendanceMap,
                    focusOfTheDay = "Regular Class" // You can add a UI input for this later
                )
                Log.d("HomeViewModel", "Auto-saved to Firestore")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save: ${e.message}") }
            }
        }
    }

    // Duolingo-style progress bar calculation
    private fun calculateProgress(total: Int, remaining: Int): Float {
        if (total == 0) return 0f
        val completed = total - remaining
        return completed.toFloat() / total.toFloat()
    }
}