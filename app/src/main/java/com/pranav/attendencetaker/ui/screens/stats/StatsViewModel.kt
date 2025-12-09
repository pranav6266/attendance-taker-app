package com.pranav.attendencetaker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.model.DailyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel : ViewModel() {
    private val repo = FirestoreRepository()
    private val _logs = MutableStateFlow<List<DailyLog>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            _logs.value = repo.getAllLogs()
        }
    }
}