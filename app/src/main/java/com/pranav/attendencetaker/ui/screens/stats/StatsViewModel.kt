package com.pranav.attendencetaker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.DailyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

class StatsViewModel : ViewModel() {
    private val repo = FirestoreRepository()

    private val _allLogs = MutableStateFlow<List<DailyLog>>(emptyList())
    private val _logs = MutableStateFlow<List<DailyLog>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    init {
        viewModelScope.launch {
            val fetchedLogs = repo.getAllLogs()
            _allLogs.value = fetchedLogs
            filterLogsForMonth(_currentMonth.value)
        }
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        filterLogsForMonth(_currentMonth.value)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        filterLogsForMonth(_currentMonth.value)
    }

    // --- NEW FUNCTION ---
    fun jumpToMonth(month: YearMonth) {
        _currentMonth.value = month
        filterLogsForMonth(month)
    }

    private fun filterLogsForMonth(month: YearMonth) {
        val prefix = month.toString()
        _logs.value = _allLogs.value.filter { it.id.startsWith(prefix) }
    }
}