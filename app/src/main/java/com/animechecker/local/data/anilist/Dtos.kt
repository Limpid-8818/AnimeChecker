package com.animechecker.local.data.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AniList GraphQL 响应模型。
 *
 * 约定：
 * - GraphQL 顶层对象为 `{ data, errors }`，由 [Envelope] 承载：成功取 data，失败看 errors；
 * - 服务端会省略空集合与空字段，故所有集合、可空字段一律给默认值；
 * - 字段名与 GraphQL 同形，只有 `Page` / `Media` 两个顶层键需要别名。
 */

/** 响应信封。 */
@Serializable
internal data class Envelope<T>(
    val data: T? = null,
    val errors: List<GqlError> = emptyList(),
)

@Serializable
internal data class GqlError(
    val message: String = "",
    val status: Int? = null,
)

/** `{ "Page": { ... } }`。 */
@Serializable
internal data class PageData(
    @SerialName("Page") val page: PageDto? = null,
)

/** `{ "Media": { ... } }`。 */
@Serializable
internal data class MediaData(
    @SerialName("Media") val media: MediaDto? = null,
)

@Serializable
internal data class PageDto(
    val media: List<MediaDto> = emptyList(),
    val airingSchedules: List<ScheduleDto> = emptyList(),
    /** 分页唯一依据；服务端 `total` 是 5000 上限值，不作判断依据。 */
    val pageInfo: PageInfoDto? = null,
)

@Serializable
internal data class PageInfoDto(
    val hasNextPage: Boolean = false,
)

/** 放送表行；[airingAt] 为 epoch 秒，缺失时由条目自身的下一集时间兜底。 */
@Serializable
internal data class ScheduleDto(
    val airingAt: Long? = null,
    val media: MediaDto? = null,
)

/**
 * 条目。[nextAiringEpisode] 的两个字段均为 epoch 秒；
 * [averageScore] 是 0-100 整数，映射时换算为 10 分制。
 */
@Serializable
internal data class MediaDto(
    val id: Long = 0,
    val episodes: Int? = null,
    val averageScore: Int? = null,
    val coverImage: CoverImageDto? = null,
    val title: TitleDto? = null,
    /** HTML 片段，落库前清理成纯文本。 */
    val description: String? = null,
    val startDate: FuzzyDateDto? = null,
    val nextAiringEpisode: NextAiringEpisodeDto? = null,
    /** WINTER / SPRING / SUMMER / FALL。 */
    val season: String? = null,
    val seasonYear: Int? = null,
    val isAdult: Boolean? = null,
)

@Serializable
internal data class CoverImageDto(val large: String? = null)

@Serializable
internal data class TitleDto(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
)

/** 首播日期：AniList 的 FuzzyDate，年月日可部分缺失。 */
@Serializable
internal data class FuzzyDateDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)

/** 下一集；[airingAt] 为 epoch 秒。 */
@Serializable
internal data class NextAiringEpisodeDto(
    val episode: Int? = null,
    val airingAt: Long? = null,
)
