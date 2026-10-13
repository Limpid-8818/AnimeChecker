package com.animechecker.local.ui.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animechecker.local.data.repository.AppRepository
import com.animechecker.local.domain.model.Quarter
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExploreViewModel (private val repository: AppRepository): ViewModel() {
    val subjects = repository.subjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seasonOptions: List<Season> = seasonList()

    // 状态管理
    var selectedSeason by mutableStateOf(Season.of(LocalDate.now()))
        private set

    var keyword by mutableStateOf("")
        private set

    var searchItems by mutableStateOf<List<Subject>?>(null) // null = 未在搜索态
        private set

    var searchFromCache by mutableStateOf(false)
        private set

    var offline by mutableStateOf(false)
        private set

    var offlineDetail by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    /** 在途的目录/搜索请求；换季度或再次搜索时取消。 */
    private var requestJob: Job? = null

    /** 请求代次，见 [startRequest]。 */
    private var requestGeneration = 0

    /** 在途请求的类型，用于判断"中途离开"要不要补拉。 */
    private var inFlightKind: RequestKind? = null

    /** 季度目录拉到一半就离开，下次进页面要补拉。 */
    private var seasonStale = false

    fun selectSeason(season: Season) {
        clearSearch()
        selectedSeason = season
        refreshSeason(season)
    }

    fun updateKeyword(newKeyword: String) {
        keyword = newKeyword
        if (newKeyword.isBlank()) {
            clearSearch()
        }
    }

    fun search() {
        val kw = keyword.trim()
        if (kw.length < 2) {
            searchItems = emptyList()
            return
        }
        val generation = startRequest(RequestKind.SEARCH)
        requestJob = viewModelScope.launch {
            loading = true
            try {
                val result = repository.search(kw)
                if (generation == requestGeneration) {
                    searchItems = result.items
                    searchFromCache = result.fromCache
                }
            } finally {
                if (generation == requestGeneration) loading = false
            }
        }
    }

    fun clearSearch() {
        // 清空搜索时要连同在途搜索一起丢掉
        invalidateRequest()
        keyword = ""
        searchItems = null
    }

    /**
     * 刷新某个指定季度。
     */
    fun refreshSeason(season: Season = selectedSeason) {
        val generation = startRequest(RequestKind.SEASON)
        requestJob = viewModelScope.launch {
            loading = true
            try {
                val result = repository.refreshSeason(season)
                if (generation == requestGeneration) {
                    offline = result.isFailure
                    offlineDetail = result.exceptionOrNull()?.message
                }
            } finally {
                if (generation == requestGeneration) loading = false
            }
        }
    }

    /**
     * 离开本页丢掉在途请求。
     */
    fun cancelInFlight() {
        if (requestJob?.isActive == true && inFlightKind == RequestKind.SEASON) seasonStale = true
        invalidateRequest()
    }

    /** 取走"季度目录上次被取消"的标记，取过即清。 */
    fun consumeSeasonStale(): Boolean = seasonStale.also { seasonStale = false }

    /**
     * 作废在途请求，取消它并推进代次，使它的收尾失效。
     */
    private fun invalidateRequest() {
        requestJob?.cancel()
        requestGeneration++
        loading = false
    }

    /**
     * 发起新请求：先取消在途的请求，再申领新的代次。
     *
     * 代次仍是自己的任务，说明没有新请求被触发，才有权写结果与复位状态。
     */
    private fun startRequest(kind: RequestKind): Int {
        invalidateRequest()
        inFlightKind = kind
        return requestGeneration
    }

    /** 在途请求的类型。 */
    private enum class RequestKind { SEASON, SEARCH }

    /**
     * 生成最近 4 个季度的列表
     */
    private fun seasonList(): List<Season> {
        val current = Season.of(LocalDate.now())
        val list = mutableListOf(current)
        var temp = current
        repeat(3) {
            temp = previousSeason(temp)
            list.add(temp)
        }
        return list
    }

    private fun previousSeason(season: Season): Season {
        return when (season.quarter) {
            Quarter.WINTER -> Season(season.year - 1, Quarter.FALL)
            Quarter.SPRING -> Season(season.year, Quarter.WINTER)
            Quarter.SUMMER -> Season(season.year, Quarter.SPRING)
            Quarter.FALL -> Season(season.year, Quarter.SUMMER)
        }
    }
}