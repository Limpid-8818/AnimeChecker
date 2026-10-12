package com.animechecker.local.ui.detail

import com.animechecker.local.domain.model.Subject
import com.animechecker.local.ui.Strings

/**
 * 简介展示内容。
 *
 * [canToggle] 为 true 表示译文与原文都在库里，界面给出原文/译文切换入口——译文质量不佳或需要
 * 核对原名时可直接看原文；只有一份时按可得的那份显示，不做切换。
 */
internal data class SummaryDisplay(
    val text: String,
    val canToggle: Boolean,
    /** 无译文只能显示原文，界面标注来源。 */
    val showOriginalNote: Boolean,
)

/**
 * 简介取文：默认显示译文，[showOriginal] 为 true 显示英文原文。
 *
 * 译文缺失时回退原文；两份都缺时给占位。
 */
internal fun summaryDisplay(subject: Subject, showOriginal: Boolean): SummaryDisplay {
    val translated = subject.summary?.cleanSummary()
    val original = subject.summaryEn?.cleanSummary()
    val canToggle = translated != null && original != null
    val text = when {
        canToggle && showOriginal -> original
        translated != null -> translated
        original != null -> original
        else -> null
    }
    return SummaryDisplay(
        text = text ?: Strings.SUMMARY_EMPTY,
        canToggle = canToggle,
        showOriginalNote = translated == null && original != null,
    )
}

/** 数据行三格文案：已放送 / 总集数 / 首播，未知项使用占位符。 */
internal data class InfoCells(val aired: String, val total: String, val firstAir: String)

internal fun infoCells(subject: Subject, aired: Int?): InfoCells = InfoCells(
    aired = when (aired) {
        null -> Strings.AIRED_EPISODES_UNKNOWN
        0 -> Strings.NOT_AIRED
        else -> Strings.AIRED_EPISODES.format(aired)
    },
    total = subject.epsTotal?.let { Strings.TOTAL_EPS.format(it) } ?: Strings.TOTAL_EPS_UNKNOWN,
    firstAir = subject.airDate?.let { Strings.FIRST_AIR.format(it) } ?: Strings.FIRST_AIR_UNKNOWN,
)

/**
 * AniList 的简介用空行分段、且尾部常留空行；空行在窄卡片里只会白占一行
 * （折叠三行时第三行会被空行吃掉，只剩一个孤立的省略号），去空白后为空即视为没有内容。
 */
private fun String.cleanSummary(): String? = lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n")
    .takeIf { it.isNotEmpty() }
