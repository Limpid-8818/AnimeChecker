package com.animechecker.local.ui.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animechecker.local.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.collections.emptyList

class CalenderViewModel(private val repository: AppRepository) : ViewModel() {

    // StateFlow
    val subjects = repository.subjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tracks = repository.tracksWithSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 状态管理
    var selectedDay by mutableIntStateOf(LocalDate.now().dayOfWeek.value)
        private set

    var offline by mutableStateOf(false)
        private set

    var offlineDetail by mutableStateOf<String?>(null)
        private set

    var syncing by mutableStateOf(false)
        private set

    val today: Int get() = LocalDate.now().dayOfWeek.value

    // 选中某天
    fun updateSelectedDay(day: Int) {
        selectedDay = day
    }

    // 刷新
    fun refresh() {
        viewModelScope.launch {
            syncing = true
            try {
                applyResult(repository.refreshCatalog())
            } finally {
                syncing = false
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncing = true
            try {
                applyResult(repository.refreshCatalog(force = true))
            } finally {
                syncing = false
            }
        }
    }

    private fun applyResult(result: Result<Unit>) {
        offline = result.isFailure
        offlineDetail = result.exceptionOrNull()?.message
    }
}