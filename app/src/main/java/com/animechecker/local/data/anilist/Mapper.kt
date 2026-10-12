package com.animechecker.local.data.anilist

import com.animechecker.local.domain.AiredEps
import com.animechecker.local.domain.model.Quarter
import com.animechecker.local.domain.model.Season
import com.animechecker.local.domain.model.Subject
import java.time.LocalDate
import kotlin.math.round

/**
 * AniList DTO ↔ 领域模型映射。
 *
 * 约定：
 * - 只做单条映射；与旧值合并并持久化由 data/local 的 `Subject.toEntity` 完成；
 * - 字段缺失则自动填充领域模型的默认值，越界数据按缺失处理，不抛异常。
 */

private val BR_TAG = Regex("(?i)<br\\s*/?>")
private val HTML_TAG = Regex("</?[^>]+>")

/**
 * 条目映射。
 *
 * [scheduleEpoch] 是该条目在放送表中的放送时间（epoch 秒），只用于推导放送星期：
 * 放送表时间 > 下一集时间 > 首播日期。
 */
internal fun MediaDto.toSubject(scheduleEpoch: Long? = null): Subject {
    val date = startDate?.toLocalDate()
    return Subject(
        id = id,
        name = title?.romaji ?: title?.english ?: title?.native ?: "AniList #$id",
        summaryEn = description?.let(::cleanDescription),
        coverUrl = coverImage?.large,
        airDate = date,
        airWeekday = weekDay(scheduleEpoch, date),
        epsTotal = episodes,
        nextEpisode = nextAiringEpisode?.episode,
        nextAiringAt = nextAiringEpisode?.airingAt,
        ratingScore = averageScore?.let { round(it / 10.0 * 10) / 10.0 },
        season = seasonOf(season, seasonYear) ?: date?.let(Season::of),
    )
}

/** 放送表行：成人向条目返回 null（不落库）。 */
internal fun ScheduleDto.toSubjectOrNull(): Subject? {
    val item = media ?: return null
    if (item.isAdult == true) return null
    return item.toSubject(scheduleEpoch = airingAt)
}

/** 放送星期：放送表时间 > 下一集时间 > 首播日期；三者都缺为 null。 */
private fun MediaDto.weekDay(scheduleEpoch: Long?, airDate: LocalDate?): Int? {
    val epoch = scheduleEpoch ?: nextAiringEpisode?.airingAt
    return when {
        epoch != null -> AiredEps.epochWeekday(epoch)
        airDate != null -> airDate.dayOfWeek.value
        else -> null
    }
}

/** AniList 季度枚举 + 年份 → 领域季度；认不出的取值返回 null，由调用方回落首播日期。 */
private fun seasonOf(season: String?, year: Int?): Season? {
    val quarter = when (season) {
        "WINTER" -> Quarter.WINTER
        "SPRING" -> Quarter.SPRING
        "SUMMER" -> Quarter.SUMMER
        "FALL" -> Quarter.FALL
        else -> return null
    }
    return year?.let { Season(it, quarter) }
}

/** 首播日期：年月日齐全且合法才有意义（AniList 允许只给年份）。 */
internal fun FuzzyDateDto.toLocalDate(): LocalDate? {
    val y = year ?: return null
    val m = month ?: return null
    val d = day ?: return null
    return runCatching { LocalDate.of(y, m, d) }.getOrNull()
}

/** AniList 简介是 HTML 片段：换行保留，其余标签与常见实体进行清理。 */
internal fun cleanDescription(raw: String): String = raw
    .replace(BR_TAG, "\n")
    .replace(HTML_TAG, "")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#039;", "'")
    .replace("&mdash;", "—")
    .replace("&nbsp;", " ")
    .trim()

/** 领域季度 → GraphQL MediaSeason 枚举名（季度目录查询变量用）。 */
internal fun Season.toAniListSeason(): String = when (quarter) {
    Quarter.WINTER -> "WINTER"
    Quarter.SPRING -> "SPRING"
    Quarter.SUMMER -> "SUMMER"
    Quarter.FALL -> "FALL"
}
