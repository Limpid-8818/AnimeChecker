package com.animechecker.local.domain.model

import java.time.LocalDate

/**
 * 番剧放送季度划分：
 * q1=WINTER（1-3 月）、q2=SPRING（4-6 月）、q3=SUMMER（7-9 月）、q4=FALL（10-12 月）。
 */
enum class Quarter {
    WINTER,
    SPRING,
    SUMMER,
    FALL;

    /** 季度序号（1-4）。 */
    val number: Int get() = ordinal + 1

    companion object {
        fun ofMonth(monthValue: Int): Quarter = entries[(monthValue - 1) / 3]
    }
}

/**
 * 季度领域类型。tag（"2026q4"）为存储与序列化格式。
 */
data class Season(
    val year: Int,
    val quarter: Quarter,
) : Comparable<Season> {

    val tag: String get() = "${year}q${quarter.number}"

    override fun compareTo(other: Season): Int =
        compareValuesBy(this, other, { it.year }, { it.quarter })

    companion object {

        fun fromTag(tag: String): Season? {
            val year = tag.substringBefore('q').toIntOrNull() ?: return null
            val quarter = tag.substringAfter('q', "").toIntOrNull() ?: return null
            if (quarter !in 1..4) return null
            return Season(year, Quarter.entries[quarter - 1])
        }

        fun of(date: LocalDate): Season =
            Season(date.year, Quarter.ofMonth(date.monthValue))
    }
}
