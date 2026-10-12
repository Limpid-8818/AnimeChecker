package com.animechecker.local.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animechecker.local.data.repository.AppError
import com.animechecker.local.data.repository.AppRepository
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.domain.model.TrackEntry
import com.animechecker.local.domain.model.TrackStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 详情页状态：条目缓存 + 追番记录，两者都直读 Room。
 *
 * [loaded] 为 false 表示 Room 首次读出前：此时既不能断言"不在清单"，也不能断言"条目不存在"，
 * 界面此时不渲染内容，避免闪现"加入追番"按钮与错误态。
 */
data class DetailUiState(
    val loaded: Boolean = false,
    val subject: Subject? = null,
    val track: TrackEntry? = null,
)

/**
 * 详情页状态：读取条目与追番记录，并把集数、状态、评分、备注的操作转交 AppRepository。
 *
 * 加载 = 打开详情时的一次网络拉取后合并写入 Room；缓存命中即立刻可见，不设整页网络 loading 门槛。
 */
class DetailViewModel(
    private val repository: AppRepository,
    private val subjectId: Long,
) : ViewModel() {

    val state: StateFlow<DetailUiState> = combine(
        repository.subject(subjectId),
        repository.trackOf(subjectId),
    ) { subject, track -> DetailUiState(loaded = true, subject = subject, track = track) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    /**
     * 最近一次详情拉取失败的原因。缓存命中时 [AppRepository.loadDetail] 视为成功，此处为 null：
     * Room 是界面唯一数据源，缓存照常展示，界面只在条目完全缺失时用它给错误态。
     */
    var loadError by mutableStateOf<AppError?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            loadError = repository.loadDetail(subjectId).exceptionOrNull() as? AppError
        }
    }

    suspend fun add() = repository.addTrack(subjectId)

    /** +1：返回 false 表示已到已放送上限，界面提示。 */
    suspend fun increment(localId: Long): Boolean = repository.increment(localId)

    suspend fun decrement(localId: Long) = repository.decrement(localId)

    suspend fun toggleEpisode(localId: Long, episode: Int) = repository.toggleEpisode(localId, episode)

    /** 追平：返回 false 表示放送集数未知，界面提示。 */
    suspend fun catchup(localId: Long): Boolean = repository.catchup(localId)

    suspend fun setStatus(localId: Long, status: TrackStatus) = repository.setStatus(localId, status)

    suspend fun setRating(localId: Long, rating: Int) = repository.setRating(localId, rating)

    suspend fun setComment(localId: Long, comment: String) = repository.setComment(localId, comment)

    suspend fun removeTrack(localId: Long) = repository.removeTrack(localId)
}
