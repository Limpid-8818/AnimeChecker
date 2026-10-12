package com.animechecker.local.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.animechecker.local.domain.model.TrackEntry
import com.animechecker.local.domain.model.TrackStatus

/** 追番记录：本地写入，无同步标记；一个条目至多一条（以 subjectId 为唯一索引）。 */
@Entity(tableName = "tracks", indices = [Index(value = ["subjectId"], unique = true)])
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val subjectId: Long,
    val status: String = TrackStatus.DOING.raw,
    /** 已看集集合的 CSV，升序。 */
    val watchedEpisodes: String = "",
    val rating: Int? = null,
    val comment: String? = null,
    val updatedAt: Long = 0,
)

fun TrackEntity.toDomain(): TrackEntry = TrackEntry(
    localId = localId,
    subjectId = subjectId,
    status = TrackStatus.fromRaw(status),
    watched = parseWatchedCsv(watchedEpisodes),
    rating = rating,
    comment = comment,
    updatedAt = updatedAt,
)

fun TrackEntry.toEntity(): TrackEntity = TrackEntity(
    localId = localId,
    subjectId = subjectId,
    status = status.raw,
    watchedEpisodes = watched.sorted().joinToString(","),
    rating = rating,
    comment = comment,
    updatedAt = updatedAt,
)

/**
 * 解析持久化的 CSV 数据为已观看剧集的集合。
 */
internal fun parseWatchedCsv(csv: String): Set<Int> =
    csv.takeIf { it.isNotBlank() }
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.toSet()
        ?: emptySet()
