package com.animechecker.local.data.repository

import com.animechecker.local.data.anilist.AniListClient
import com.animechecker.local.data.llm.LlmTranslator
import com.animechecker.local.data.llm.TranslationQueue
import com.animechecker.local.data.local.db.AppDatabase
import com.animechecker.local.data.local.entity.SubjectEntity
import com.animechecker.local.data.local.entity.toDomain
import com.animechecker.local.data.local.entity.toEntity
import com.animechecker.local.data.settings.SettingsStore
import com.animechecker.local.domain.AiredEps
import com.animechecker.local.domain.model.LlmSettings
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.domain.model.TrackEntry
import com.animechecker.local.domain.model.TrackStatus
import com.animechecker.local.domain.model.TrackedSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 数据层唯一门面：UI 经由本类读写全部业务状态，不绕过它触碰任何数据源。
 *
 * 职责：
 * - Room 是界面的唯一数据源，UI 状态一律来自 Flow 直读；
 * - 网络只做「拉取后合并写入 Room」，不持有独立状态，不构成页面 loading 门槛；
 * - 刷新分层：首页只做追番对象轻量刷新（[refreshTracked]，空清单零网络），目录类整页拉取（[refreshCatalog]）只在放送 / 番库页触发；
 * - 本地写操作（勾集、入退番、评分评论）只写 Room，离线全功能可用；
 * - 翻译是后台任务：上游数据写入后触发一轮（[kickTranslation]），不绑定页面生命周期；
 *   未配置翻译服务时为空操作，标题回退罗马音、简介回退英文原文。
 *
 * 失败以 [Result] 承载 [AppError]，由 UI 映射成文案；协程取消不算失败，继续向上传播。
 */
class AppRepository internal constructor(
    database: AppDatabase,
    private val anilist: AniListClient,
    private val llm: LlmTranslator,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val subjectDao = database.subjectDao()
    private val trackDao = database.trackDao()

    private val refreshMutex = Mutex()
    private val translationMutex = Mutex()
    private val translations = TranslationQueue(RoomTranslationQueueStore(subjectDao), llm)
    private var lastCatalogRefreshAt = 0L
    private var lastTrackedRefreshAt = 0L

    /** 搜索结果：[fromCache] 表示网络失败后回落到本地模糊匹配。 */
    data class SearchResult(val items: List<Subject>, val fromCache: Boolean)

    // ---------- 读（Room 直读） ----------

    /** 首页行：追番记录 + 条目缓存（缓存可缺失，[TrackedSubject.subject] 为空）。 */
    fun tracksWithSubjects(): Flow<List<TrackedSubject>> =
        combine(trackDao.observeAll(), subjectDao.observeAll()) { tracks, subjects ->
            val byId = subjects.associateBy { it.id }
            tracks.map { TrackedSubject(it.toDomain(), byId[it.subjectId]?.toDomain()) }
        }

    /** 库内全部条目：番库页与季度页按领域模型过滤。 */
    fun subjects(): Flow<List<Subject>> = subjectDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun subject(id: Long): Flow<Subject?> = subjectDao.observeById(id).map { it?.toDomain() }

    fun trackOf(subjectId: Long): Flow<TrackEntry?> = trackDao.observeBySubject(subjectId).map { it?.toDomain() }

    fun translationPendingCount(): Flow<Int> = subjectDao.translationPendingCount()

    fun translationFailedCount(): Flow<Int> = subjectDao.translationFailedCount()

    /** 设置页「上次刷新」时间。 */
    fun lastRefreshAt(): Flow<Long> = settingsStore.lastRefreshAtFlow

    // ---------- 上游拉取 ----------

    /**
     * 目录刷新（放送 / 番库页）：拉放送表，并对追番对象批量补详情。
     * 受 AniList 限流影响一次 10-25 秒，只在有自身 loading 态的页面调用；
     * [force] 为 false 时 [REFRESH_COOLDOWN_MS] 内不重复拉取。
     */
    suspend fun refreshCatalog(force: Boolean = false): Result<Unit> = refreshMutex.withLock {
        if (!force && now() - lastCatalogRefreshAt < REFRESH_COOLDOWN_MS) {
            return@withLock Result.success(Unit)
        }
        resultOf {
            withContext(Dispatchers.IO) {
                val schedule = anilist.schedule()
                upsertMerged(schedule)
                // 日历成员资格只认本次放送窗口：窗口外（含已完结）一律摘出
                if (schedule.isNotEmpty()) {
                    val ids = schedule.map { it.id }
                    subjectDao.markInCalendar(ids)
                    subjectDao.clearInCalendarExcept(ids)
                } else {
                    subjectDao.clearAllInCalendar()
                }
                val trackedIds = trackDao.all().map { it.subjectId }.distinct()
                if (trackedIds.isNotEmpty()) {
                    // 追番对象详情尽力而为：失败不影响放送表结果，取消仍需向上传播
                    resultOf { upsertMerged(anilist.details(trackedIds), summariesWanted = true) }
                }
            }
            lastCatalogRefreshAt = now()
            settingsStore.setLastRefreshAt(lastCatalogRefreshAt)
        }
    }.also { kickTranslation() }

    /**
     * 首页轻量刷新：只更新追番对象的进度数据。
     */
    suspend fun refreshTracked(): Result<Unit> = refreshMutex.withLock {
        if (now() - lastTrackedRefreshAt < REFRESH_COOLDOWN_MS) {
            return@withLock Result.success(Unit)
        }
        resultOf {
            withContext(Dispatchers.IO) {
                val trackedIds = trackDao.all().map { it.subjectId }.distinct()
                if (trackedIds.isNotEmpty()) {
                    upsertMerged(anilist.details(trackedIds), summariesWanted = true)
                }
            }
        }.onSuccess { lastTrackedRefreshAt = now() }
    }.also { kickTranslation() }

    /** 详情：网络优先并缓存；失败时回退缓存。打开详情本身即简介入翻译队列的信号。 */
    suspend fun loadDetail(id: Long): Result<Subject> {
        val fresh = resultOf {
            withContext(Dispatchers.IO) {
                upsertMerged(listOf(anilist.detail(id)), summariesWanted = true)
            }
        }
        kickTranslation()
        val cached = subjectDao.byId(id)?.toDomain()
        // 拉取失败时上一步的入队没跑到，缓存里有原文也算"打开过详情"，补一次入队
        if (fresh.isFailure && cached?.summaryEn != null) {
            withContext(Dispatchers.IO) { subjectDao.markNeedSummary(id) }
        }
        return when {
            cached != null -> Result.success(cached)
            else -> Result.failure(fresh.exceptionOrNull()?.toAppError() ?: AppError(AppError.Kind.NOT_FOUND))
        }
    }

    /** 搜索：网络优先并缓存；失败降级本地模糊匹配。关键词少于 [MIN_SEARCH_LENGTH] 个字符时不查。 */
    suspend fun search(keyword: String): SearchResult {
        val trimmed = keyword.trim()
        if (trimmed.length < MIN_SEARCH_LENGTH) return SearchResult(emptyList(), fromCache = false)
        val remote = resultOf {
            withContext(Dispatchers.IO) {
                val items = anilist.search(trimmed)
                upsertMerged(items)
                items.mapNotNull { subjectDao.byId(it.id)?.toDomain() }
            }
        }
        kickTranslation()
        return remote.fold(
            onSuccess = { SearchResult(it, fromCache = false) },
            onFailure = { SearchResult(subjectDao.search(trimmed).map { it.toDomain() }, fromCache = true) },
        )
    }

    /** 季度目录：拉取后入缓存，界面从 [subjects] 的 Flow 过滤。 */
    suspend fun refreshSeason(season: Season): Result<Unit> = resultOf {
        withContext(Dispatchers.IO) { upsertMerged(anilist.season(season)) }
    }.also { kickTranslation() }

    /**
     * 合并写：网络非空字段覆盖旧值、空字段保留，nextEpisode / nextAiringAt 例外
     * （详见 `Subject.toEntity`）；中文标题与简介只来自翻译器，网络侧不触碰。
     *
     * [summariesWanted] 表示这批条目的简介是否入翻译队列：详情与追番刷新为 true，
     * 放送表、季度、搜索为 false（控制 LLM 调用量）。
     */
    private suspend fun upsertMerged(incoming: List<Subject>, summariesWanted: Boolean = false) {
        if (incoming.isEmpty()) return
        val syncedAt = now()
        subjectDao.upsertAll(incoming.map { it.toEntity(syncedAt, subjectDao.byId(it.id)) })
        if (summariesWanted) {
            val wanted = incoming.filter { it.summaryEn != null }.map { it.id }
            if (wanted.isNotEmpty()) subjectDao.markNeedSummaryAll(wanted)
        }
    }

    // ---------- 翻译 ----------

    /** 后台触发一轮翻译；未配置翻译服务时为空操作。重复触发由互斥锁串行。 */
    fun kickTranslation() {
        scope.launch {
            val settings = settingsStore.llmSettings()
            if (!settings.usable) return@launch
            translationMutex.withLock { translations.pump(settings) }
        }
    }

    /** 设置页「重试失败项」：FAILED 全部复位 PENDING 并触发一轮，返回复位条数。 */
    suspend fun retryFailedTranslations(): Int {
        val reset = subjectDao.resetFailedTranslations()
        if (reset > 0) kickTranslation()
        return reset
    }

    /** 设置页：当前翻译服务配置。 */
    fun llmSettings(): Flow<LlmSettings> = settingsStore.llmSettingsFlow

    /** 设置页保存配置。持久化后立即尝试补译已有条目。 */
    suspend fun saveLlmSettings(settings: LlmSettings) {
        settingsStore.saveLlmSettings(settings)
        kickTranslation()
    }

    /** 设置页「测试连接」：用编辑中的配置试一次，失败按领域错误返回。 */
    suspend fun testLlmConnection(settings: LlmSettings): Result<Unit> = resultOf {
        withContext(Dispatchers.IO) { llm.test(settings) }
    }

    // ---------- 写（本地即时生效） ----------

    suspend fun addTrack(subjectId: Long, status: TrackStatus = TrackStatus.DOING) {
        if (trackDao.bySubject(subjectId) != null) return
        trackDao.insert(
            TrackEntry(subjectId = subjectId, status = status, updatedAt = now()).toEntity(),
        )
    }

    /** +1：把下一集（当前最大集 +1）加入集合。返回 false = 已到上限（已放送集数），未加。 */
    suspend fun increment(localId: Long): Boolean {
        val track = trackDao.byId(localId) ?: return false
        val subject = subjectDao.byId(track.subjectId)?.toDomain()
        val cap = AiredEps.subjectAired(subject) ?: subject?.epsTotal
        val next = (track.toDomain().watched.maxOrNull() ?: 0) + 1
        if (cap != null && next > cap) return false
        mutate(localId) { entry -> entry.copy(watched = entry.watched + next) }
        return true
    }

    /** 撤回一集：移除当前最大集。 */
    suspend fun decrement(localId: Long): Boolean {
        val track = trackDao.byId(localId) ?: return false
        val max = track.toDomain().watched.maxOrNull() ?: return false
        mutate(localId) { it.copy(watched = it.watched - max) }
        return true
    }

    /** 单集开关：勾第 n 集只影响第 n 集，不连坐前后集。 */
    suspend fun toggleEpisode(localId: Long, episode: Int) {
        mutate(localId) { entry ->
            entry.copy(watched = if (episode in entry.watched) entry.watched - episode else entry.watched + episode)
        }
    }

    suspend fun setStatus(localId: Long, status: TrackStatus) = mutate(localId) { it.copy(status = status) }

    /** 评分：超出 1..10 视为未评分。 */
    suspend fun setRating(localId: Long, rating: Int) =
        mutate(localId) { it.copy(rating = rating.takeIf { value -> value in 1..10 }) }

    /** 评论：空白视为清空。 */
    suspend fun setComment(localId: Long, comment: String) =
        mutate(localId) { it.copy(comment = comment.trim().ifBlank { null }) }

    /** 追平：已放送的集数全部加入集合。返回 false = 缺放送数据。 */
    suspend fun catchup(localId: Long): Boolean {
        val track = trackDao.byId(localId) ?: return false
        val aired = AiredEps.subjectAired(subjectDao.byId(track.subjectId)?.toDomain()) ?: return false
        mutate(localId) { it.copy(watched = (1..aired).toSet()) }
        return true
    }

    suspend fun removeTrack(localId: Long) = trackDao.delete(localId)

    /**
     * 手动编辑译文。
     * 保存为空白视为清除该字段，并标记该条目翻译状态为PENDING，会重新执行翻译。
     */
    suspend fun saveTranslation(subjectId: Long, nameCn: String?, summary: String?) {
        val subject = subjectDao.byId(subjectId) ?: return
        val name = nameCn?.trim()?.ifBlank { null }
        val translated = summary?.trim()?.ifBlank { null }
        subjectDao.setNameCn(subjectId, name)
        subjectDao.setSummary(subjectId, translated)
        val workLeft = name == null ||
            (subject.needSummary && subject.summaryEn != null && translated == null)
        when {
            !workLeft -> subjectDao.setTranslateState(subjectId, SubjectEntity.TRANSLATE_DONE)
            subject.translateState == SubjectEntity.TRANSLATE_DONE ->
                subjectDao.setTranslateState(subjectId, SubjectEntity.TRANSLATE_PENDING)
        }
    }

    private suspend fun mutate(localId: Long, transform: (TrackEntry) -> TrackEntry) {
        val entity = trackDao.byId(localId) ?: return
        trackDao.upsert(transform(entity.toDomain()).copy(updatedAt = now()).toEntity())
    }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val REFRESH_COOLDOWN_MS = 60_000L
        const val MIN_SEARCH_LENGTH = 2
    }
}

/**
 * runCatching 会把协程取消一起吞掉（切页/切季度时 LaunchedEffect 取消会被当成网络失败弹错误条），
 * 取消必须向上传播；其余失败一并收敛成 [AppError]。
 */
private inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e.toAppError())
}
