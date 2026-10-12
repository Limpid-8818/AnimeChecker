package com.animechecker.local.domain.model

/** 追番状态，分别对应想看/在看/看完/搁置/放弃。raw 为存储层使用的字符串，映射在实体一侧完成。 */
enum class TrackStatus(val raw: String) {
    WISH("WISH"),
    DOING("DOING"),
    DONE("DONE"),
    ON_HOLD("ON_HOLD"),
    DROPPED("DROPPED");

    companion object {
        /** 未知取值默认为在看。 */
        fun fromRaw(raw: String?): TrackStatus = entries.firstOrNull { it.raw == raw } ?: DOING
    }
}

/** 追番记录。watched 是已看集的集合。 */
data class TrackEntry(
    val localId: Long = 0L,
    val subjectId: Long,
    val status: TrackStatus = TrackStatus.DOING,
    val watched: Set<Int> = emptySet(),
    val rating: Int? = null,
    val comment: String? = null,
    val updatedAt: Long = 0L,
)

/** 首页行：追番记录与其条目缓存（缓存可能缺失，subject 可为空）。 */
data class TrackedSubject(
    val track: TrackEntry,
    val subject: Subject?,
)
