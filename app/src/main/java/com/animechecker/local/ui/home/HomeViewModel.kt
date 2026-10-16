package com.animechecker.local.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animechecker.local.data.repository.AppError
import com.animechecker.local.data.repository.AppRepository
import com.animechecker.local.domain.model.TrackStatus
import com.animechecker.local.domain.model.TrackedSubject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 首页状态。
 */
class HomeViewModel(private val repository: AppRepository) : ViewModel() {

    val rows: StateFlow<List<TrackedSubject>> = repository.tracksWithSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 最近一次轻量刷新的失败原因，成功即清空；界面据此显示离线提示条。 */
    var refreshError by mutableStateOf<AppError?>(null)
        private set

    /** 首页轻量刷新：只更新追番对象详情。 */
    fun refresh() {
        viewModelScope.launch {
            refreshError = repository.refreshTracked().exceptionOrNull() as? AppError
        }
    }

    /** +1：返回 false 表示已追上放送进度，由界面提示。 */
    suspend fun increment(localId: Long): Boolean = repository.increment(localId)

    /** 撤销一集：移除当前最大集。 */
    suspend fun decrement(localId: Long): Boolean = repository.decrement(localId)

    /** 单集开关：勾第 n 集不影响其它集。 */
    suspend fun toggleEpisode(localId: Long, episode: Int) =
        repository.toggleEpisode(localId, episode)
}

/** 状态计数：status 为 null 表示全部。 */
fun List<TrackedSubject>.countOf(status: TrackStatus?): Int =
    if (status == null) size else count { it.track.status == status }
