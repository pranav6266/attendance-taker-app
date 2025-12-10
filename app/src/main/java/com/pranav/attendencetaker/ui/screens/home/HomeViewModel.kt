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
    private var allCachedStudents: List<Student> = emptyList()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _attendanceMap = mutableMapOf<String, AttendanceStatus>()
    private val _actionStack = Stack<Pair<Student, AttendanceStatus>>()

    init {
        loadSession()
    }

    fun refresh() {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // 1. Fetch Students
                val initialStudents = repository.getActiveStudents()

                // 2. RECALCULATE STREAKS (The Fix)
                // This ensures streaks are always in sync with existing logs, even if you deleted one.
                val correctedStudents = repository.recalculateStreaks(initialStudents)
                allCachedStudents = correctedStudents

                // 3. Fetch Today's Log
                val todayLog = repository.getTodayLog()

                // 4. Populate Map
                if (todayLog != null) {
                    _attendanceMap.putAll(todayLog.attendance)
                }

                val remainingStudents = if (todayLog != null) {
                    correctedStudents.filter { student ->
                        !todayLog.attendance.containsKey(student.id)
                    }
                } else {
                    correctedStudents
                }

                val currentLog = getCurrentLogObj(todayLog?.finalized == true)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        studentsQueue = remainingStudents,
                        totalCount = correctedStudents.size,
                        progress = calculateProgress(correctedStudents.size, remainingStudents.size),
                        isAttendanceFinalized = todayLog?.finalized == true,
                        dailyLog = currentLog
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
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
                // 1. Save Today's Log
                repository.saveAttendanceBatch(
                    attendanceMap = _attendanceMap,
                    focusOfTheDay = "Regular Class"
                )

                // 2. Recalculate streaks again to include today's new data
                // This updates the UI and Firestore with the new streaks including today
                val updatedStudents = repository.recalculateStreaks(allCachedStudents)
                allCachedStudents = updatedStudents // Update local cache

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
            attendance = _attendanceMap.toMap(),
            finalized = isFinalized
        )
    }
}