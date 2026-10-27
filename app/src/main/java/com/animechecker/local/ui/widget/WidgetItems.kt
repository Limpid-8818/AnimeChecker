package com.animechecker.local.ui.widget

import com.animechecker.local.domain.AiredEps
import com.animechecker.local.domain.model.TrackStatus
import com.animechecker.local.domain.model.TrackedSubject
import java.time.LocalDate

/** 桌面组件卡片数据模型。 */
data class WidgetItem(
    val localId: Long,
    val subjectId: Long,
    val title: String?,
    /** 封面图地址；条目缓存缺失时为 null。 */
    val coverUrl: String?,
    val watchedCount: Int,
    val aired: Int?,
    val epsTotal: Int?,
    /** 放送星期（1-7），状态行展示用。 */
    val airWeekday: Int?,
    /** 今日放送标志位。 */
    val isToday: Boolean,
)

/**
 * 组件条目序列：与首页"在看"一致、以 Room 插入序为排序基础，并将"今日放送"的条目置顶。
 */
internal fun widgetItems(
    rows: List<TrackedSubject>,
    today: LocalDate = AiredEps.today(),
): List<WidgetItem> = rows
    .filter { it.track.status == TrackStatus.DOING }
    .map { row ->
        val subject = row.subject
        val aired = AiredEps.subjectAired(subject, today)
        WidgetItem(
            localId = row.track.localId,
            subjectId = row.track.subjectId,
            title = subject?.let { it.nameCn ?: it.name },
            coverUrl = subject?.coverUrl,
            watchedCount = row.track.watched.count(),
            aired = aired,
            epsTotal = subject?.epsTotal,
            airWeekday = subject?.airWeekday,
            isToday = subject != null &&
                subject.airWeekday == today.dayOfWeek.value &&
                unfinished(epsTotal = subject.epsTotal, aired = aired),
        )
    }
    .sortedByDescending { it.isToday }

/** 是否未完结：总集数未知或尚未更新完；放送信息缺失按未完结处理。 */
private fun unfinished(epsTotal: Int?, aired: Int?): Boolean =
    epsTotal == null || aired == null || aired < epsTotal

/** 锚点定位，若锚点不在序列中（未存储/已移出清单）时则返回首位。 */
internal fun anchorIndex(items: List<WidgetItem>, anchorSubjectId: Long?): Int =
    items.indexOfFirst { it.subjectId == anchorSubjectId }.takeIf { it >= 0 } ?: 0

/** 列表循环索引。 */
internal fun stepIndex(current: Int, size: Int, delta: Int): Int =
    if (size <= 0) 0 else (((current + delta) % size) + size) % size

/** 是否已追上：观看数达到上限（已放送集数，缺失时则使用总集数判断）；上限未知视为未追上。 */
internal fun caughtUp(watchedCount: Int, aired: Int?, epsTotal: Int?): Boolean {
    val cap = aired ?: epsTotal
    return cap != null && watchedCount >= cap
}
