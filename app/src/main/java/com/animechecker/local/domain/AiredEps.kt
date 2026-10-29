/**
 * 已放送集数推算：完结条目按总集数计；其次按 nextAiringEpisode 精确推算，
 * 不足时按放送日周更兜底，总集数封顶。
 *
 * 纯函数、调用时实时计算。
 */
package com.animechecker.local.domain

import com.animechecker.local.domain.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object AiredEps {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    fun compute(
        airDate: LocalDate?,
        nextEpisode: Int?,
        nextAiringAt: Long?,
        epsTotal: Int?,
        finished: Boolean = false,
        today: LocalDate = LocalDate.now(zone),
        nowEpoch: Long = System.currentTimeMillis() / 1000,
    ): Int? {
        // 完结路径：总集数即已放送集数
        if (finished && epsTotal != null) return epsTotal
        // 精确路径：AniList 给出下一集编号与放送时间
        if (nextEpisode != null && nextEpisode > 0 && nextAiringAt != null) {
            val base = if (nowEpoch >= nextAiringAt) nextEpisode else nextEpisode - 1
            return cap(base, epsTotal)
        }
        // 周更估算路径
        if (airDate == null) return null
        if (today.isBefore(airDate)) return 0
        val base = (ChronoUnit.DAYS.between(airDate, today) / 7 + 1).toInt()
        return cap(base, epsTotal)
    }

    private fun cap(base: Int, epsTotal: Int?): Int {
        val v = base.coerceAtLeast(0)
        return if (epsTotal != null) minOf(v, epsTotal) else v
    }

    /** 界面用：按条目领域模型推算已放送集数。 */
    fun subjectAired(subject: Subject?, today: LocalDate = LocalDate.now(zone)): Int? {
        if (subject == null) return null
        return compute(
            airDate = subject.airDate,
            nextEpisode = subject.nextEpisode,
            nextAiringAt = subject.nextAiringAt,
            epsTotal = subject.epsTotal,
            finished = subject.finished,
            today = today,
        )
    }

    /** 今日日期工具函数。 */
    fun today(): LocalDate = LocalDate.now(zone)

    /** epoch 秒 → Asia/Shanghai 星期（1-7）。 */
    fun epochWeekday(epoch: Long): Int =
        Instant.ofEpochSecond(epoch).atZone(zone).dayOfWeek.value
}