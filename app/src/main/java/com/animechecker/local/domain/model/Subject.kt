package com.animechecker.local.domain.model

import java.time.LocalDate

/** 条目：AniList 数据缓存 + 翻译结果的领域视图，UI 消费形状。 */
data class Subject(
    val id: Long,
    val name: String,
    val nameCn: String? = null,
    val summary: String? = null,
    // 简介英文原文，译文缺失时回退显示。
    val summaryEn: String? = null,
    val coverUrl: String? = null,
    val airDate: LocalDate? = null,
    // 放送星期，1-7。
    val airWeekday: Int? = null,
    val epsTotal: Int? = null,
    // 下一集编号（AniList nextAiringEpisode）。
    val nextEpisode: Int? = null,
    // 下一集放送时间
    val nextAiringAt: Long? = null,
    val ratingScore: Double? = null,
    // 放送季度。
    val season: Season? = null,
    // 是否处于放送日历窗口（仅放送表拉取维护，完结条目不进）。
    val inCalendar: Boolean = false,
    val translateState: TranslateState = TranslateState.PENDING,
)

/** 翻译状态。fromCode 的取值为存储层整数编码。 */
enum class TranslateState {
    PENDING,
    DONE,
    FAILED;

    companion object {
        fun fromCode(code: Int): TranslateState = when (code) {
            1 -> DONE
            2 -> FAILED
            else -> PENDING
        }
    }
}
