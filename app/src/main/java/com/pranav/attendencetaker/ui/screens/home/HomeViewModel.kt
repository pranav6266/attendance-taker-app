package com.pranav.attendencetaker.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Stack

class HomeViewModel : ViewModel() {

    private val repository = FirestoreRepository()
    private val calculateStreak = com.pranav.attendencetaker.domain.CalculateStreakUseCase()
    private var allCachedStudents: List<Student> = emptyList()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _attendanceMap = mutableMapOf<String, AttendanceStatus>()
    private val _actionStack = Stack<Pair<Student, AttendanceStatus>>()

    init {
        loadSession()
    }

    // Called when user returns to screen (e.g., from Details) to refresh finalized status
    fun refresh() {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val allStudents = repository.getActiveStudents()
            allCachedStudents = allStudents
            val todayLog = repository.getTodayLog()

            // Populate local map with existing data
            if (todayLog != null) {
                _attendanceMap.putAll(todayLog.attendance)
            }

            val remainingStudents = if (todayLog != null) {
                allStudents.filter { student ->
                    !todayLog.attendance.containsKey(student.id)
                }
            } else {
                allStudents
            }

            val currentLog = getCurrentLogObj(todayLog?.finalized == true)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    studentsQueue = remainingStudents,
                    totalCount = allStudents.size,
                    progress = calculateProgress(allStudents.size, remainingStudents.size),
                    isAttendanceFinalized = todayLog?.finalized == true,
                    dailyLog = currentLog
                )
            }
        }
    }

    fun onSwipe(student: Student, status: AttendanceStatus) {
        _attendanceMap[student.id] = status
        _actionStack.push(Pair(student, status))

        _uiState.update { currentState ->
            val newQueue = currentState.studentsQueue - student
            val isQueueEmpty = newQueue.isEmpty()

            if (isQueueEmpty) saveToBackend()

            currentState.copy(
                studentsQueue = newQueue,
                progress = calculateProgress(currentState.totalCount, newQueue.size),
                dailyLog = getCurrentLogObj(currentState.isAttendanceFinalized)
            )
        }
    }

    fun onUndo() {
        if (_actionStack.isEmpty()) return

        val lastAction = _actionStack.pop()
        val student = lastAction.first

        _attendanceMap.remove(student.id)

        _uiState.update { currentState ->
            val newQueue = listOf(student) + currentState.studentsQueue

            currentState.copy(
                studentsQueue = newQueue,
                progress = calculateProgress(currentState.totalCount, newQueue.size),
                dailyLog = getCurrentLogObj(currentState.isAttendanceFinalized)
            )
        }
    }

    private fun saveToBackend() {
        viewModelScope.launch {
            try {
                // 1. Save the Daily Log (Existing logic)
                repository.saveAttendanceBatch(
                    attendanceMap = _attendanceMap,
                    focusOfTheDay = "Regular Class"
                )

                // 2. NEW: Calculate and Save Streaks
                val streakUpdates = mutableMapOf<String, Pair<Int, Long>>()
                val now = System.currentTimeMillis()

                // Filter for students who are PRESENT or LATE
                _attendanceMap.filter {
                    it.value == AttendanceStatus.PRESENT || it.value == AttendanceStatus.LATE
                }.forEach { (studentId, _) ->
                    val student = allCachedStudents.find { it.id == studentId }
                    if (student != null) {
                        val newStreak = calculateStreak(student.currentStreak, student.lastAttendedDate)
                        streakUpdates[studentId] = Pair(newStreak, now)
                    }
                }

                if (streakUpdates.isNotEmpty()) {
                    repository.updateStudentStreaks(streakUpdates)
                }

                Log.d("HomeViewModel", "Auto-saved log and updated streaks")

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save: ${e.message}") }
            }
        }
    }

    private fun calculateProgress(total: Int, remaining: Int): Float {
        if (total == 0) return 0f
        val completed = total - remaining
        return completed.toFloat() / total.toFloat()
    }

    private fun getCurrentLogObj(isFinalized: Boolean): DailyLog {
        return DailyLog(
            id = repository.getTodayId(),
            date = Date(),
            attendance = _attendanceMap.toMap(), // Pass copy of map
            finalized = isFinalized
        )
    }
}