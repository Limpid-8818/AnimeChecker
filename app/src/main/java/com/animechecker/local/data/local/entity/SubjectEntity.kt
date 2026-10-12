package com.animechecker.local.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import com.animechecker.local.domain.model.TranslateState
import java.time.LocalDate

/**
 * 条目缓存：AniList 拉取合并的目标，含翻译状态与队列标记。
 * 已放送集数不进数据库，由 domain.AiredEps 实时推算。
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val nameCn: String? = null,
    val summary: String? = null,
    val summaryEn: String? = null,
    val coverUrl: String? = null,
    /** yyyy-MM-dd，周更推算兜底用。 */
    val airDate: String? = null,
    /** 1-7，放送星期。 */
    val airWeekday: Int? = null,
    val epsTotal: Int? = null,
    val nextEpisode: Int? = null,
    val nextAiringAt: Long? = null,
    val ratingScore: Double? = null,
    /** "2026q4"，经 Season.fromTag 往返。 */
    val seasonTag: String? = null,
    /** 是否在放送日历窗口内：仅放送表拉取维护。 */
    val inCalendar: Boolean = false,
    /** 简介是否入翻译队列：仅追番对象与打开过详情的条目。 */
    val needSummary: Boolean = false,
    val translateState: Int = TRANSLATE_PENDING,
    val syncedAt: Long = 0,
) {
    companion object {
        const val TRANSLATE_PENDING = 0
        const val TRANSLATE_DONE = 1
        const val TRANSLATE_FAILED = 2
    }
}

fun SubjectEntity.toDomain(): Subject = Subject(
    id = id,
    name = name,
    nameCn = nameCn,
    summary = summary,
    summaryEn = summaryEn,
    coverUrl = coverUrl,
    airDate = airDate?.let(LocalDate::parse),
    airWeekday = airWeekday,
    epsTotal = epsTotal,
    nextEpisode = nextEpisode,
    nextAiringAt = nextAiringAt,
    ratingScore = ratingScore,
    season = seasonTag?.let(Season::fromTag),
    inCalendar = inCalendar,
    translateState = TranslateState.fromCode(translateState),
)

/**
 * 网络数据持久化：非空字段覆盖旧值，空字段保留旧值；
 * nextEpisode / nextAiringAt 例外，番播完后 AniList 会清空该字段，旧值必须跟着失效。
 * 翻译结果与状态、队列标记、日历标记只由本地写路径维护，网络侧不修改。
 */
fun Subject.toEntity(syncedAt: Long, old: SubjectEntity? = null): SubjectEntity = SubjectEntity(
    id = id,
    name = name,
    nameCn = nameCn ?: old?.nameCn,
    summary = summary ?: old?.summary,
    summaryEn = summaryEn ?: old?.summaryEn,
    coverUrl = coverUrl ?: old?.coverUrl,
    airDate = airDate?.toString() ?: old?.airDate,
    airWeekday = airWeekday ?: old?.airWeekday,
    epsTotal = epsTotal ?: old?.epsTotal,
    nextEpisode = nextEpisode,
    nextAiringAt = nextAiringAt,
    ratingScore = ratingScore ?: old?.ratingScore,
    seasonTag = season?.tag ?: old?.seasonTag,
    inCalendar = old?.inCalendar ?: false,
    needSummary = old?.needSummary ?: false,
    translateState = old?.translateState ?: SubjectEntity.TRANSLATE_PENDING,
    syncedAt = syncedAt,
)
